package de.otto.synapse.configuration.kinesis;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import de.otto.synapse.configuration.SynapseAutoConfiguration;
import de.otto.synapse.configuration.aws.AwsProperties;
import de.otto.synapse.configuration.aws.SynapseAwsAuthConfiguration;
import de.otto.synapse.endpoint.MessageInterceptorRegistry;
import de.otto.synapse.endpoint.receiver.MessageLogReceiverEndpointFactory;
import de.otto.synapse.endpoint.receiver.kinesis.KinesisMessageLogReceiverEndpointFactory;
import de.otto.synapse.endpoint.sender.MessageSenderEndpointFactory;
import de.otto.synapse.endpoint.sender.kinesis.KinesisMessageSenderEndpointFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.internal.retry.SdkDefaultRetrySetting;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.core.retry.RetryPolicyContext;
import software.amazon.awssdk.core.retry.backoff.FullJitterBackoffStrategy;
import software.amazon.awssdk.core.retry.conditions.RetryCondition;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.slf4j.LoggerFactory.getLogger;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.OPERATION_NAME;
import static software.amazon.awssdk.core.interceptor.SdkExecutionAttribute.SERVICE_NAME;

@Configuration
@Import({SynapseAwsAuthConfiguration.class, SynapseAutoConfiguration.class})
@EnableConfigurationProperties(AwsProperties.class)
public class KinesisAutoConfiguration {

    private static final Logger LOG = getLogger(KinesisAutoConfiguration.class);

    private final AwsProperties awsProperties;

    @Autowired
    public KinesisAutoConfiguration(final AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
    }

    @Bean
    @ConditionalOnMissingBean(name = "kinesisMessageLogExecutorService")
    public ExecutorService kinesisMessageLogExecutorService() {
        return Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("kinesis-message-log-%d").build()
        );

    }

    @Bean
    @ConditionalOnMissingBean(name = "kinesisRetryPolicy", value = RetryPolicy.class)
    public RetryPolicy kinesisRetryPolicy() {
        return RetryPolicy.defaultRetryPolicy().toBuilder()
                .retryCondition(new DefaultLoggingRetryCondition(5, 10))
                .numRetries(Integer.MAX_VALUE)
                .backoffStrategy(FullJitterBackoffStrategy.builder()
                        .baseDelay(Duration.ofSeconds(1))
                        .maxBackoffTime(SdkDefaultRetrySetting.MAX_BACKOFF)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(KinesisAsyncClient.class)
    public KinesisAsyncClient kinesisAsyncClient(final AwsCredentialsProvider credentialsProvider,
                                                 final RetryPolicy kinesisRetryPolicy) {
        return KinesisAsyncClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(awsProperties.getRegion()))
                .overrideConfiguration(ClientOverrideConfiguration.builder().retryPolicy(kinesisRetryPolicy).build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "messageLogSenderEndpointFactory")
    public MessageSenderEndpointFactory messageLogSenderEndpointFactory(final MessageInterceptorRegistry registry,
                                                                        final KinesisAsyncClient kinesisClient) {
        LOG.info("Auto-configuring Kinesis MessageSenderEndpointFactory");
        return new KinesisMessageSenderEndpointFactory(registry, kinesisClient);
    }

    @Bean
    @ConditionalOnMissingBean(name = "messageLogReceiverEndpointFactory")
    public MessageLogReceiverEndpointFactory messageLogReceiverEndpointFactory(final MessageInterceptorRegistry interceptorRegistry,
                                                                               final KinesisAsyncClient kinesisClient,
                                                                               final ExecutorService kinesisMessageLogExecutorService,
                                                                               final ApplicationEventPublisher eventPublisher) {
        LOG.info("Auto-configuring Kinesis MessageLogReceiverEndpointFactory");
        return new KinesisMessageLogReceiverEndpointFactory(interceptorRegistry, kinesisClient, kinesisMessageLogExecutorService, eventPublisher);
    }



    public static class DefaultLoggingRetryCondition implements RetryCondition {

        private final int warnCount;
        private final int errorCount;

        public DefaultLoggingRetryCondition(int warnCount, int errorCount) {
            this.warnCount = warnCount;
            this.errorCount = errorCount;
        }

        @Override
        public boolean shouldRetry(RetryPolicyContext context) {
            logRetryAttempt(context);
            return RetryCondition.defaultRetryCondition().shouldRetry(context);
        }

        private void logRetryAttempt(RetryPolicyContext c) {
            final String operationName = c.executionAttributes().getAttribute(OPERATION_NAME);
            final String serviceName = c.executionAttributes().getAttribute(SERVICE_NAME);

            String message;
            if (c.exception() != null) {
                message = String.format("'%s' request to '%s' failed with exception on try %s: %s", operationName, serviceName, c.retriesAttempted(), findExceptionMessage(c.exception()));
            } else {
                message = String.format("'%s' request to '%s' failed without exception on try %s:", operationName, serviceName, c.retriesAttempted());
            }

            if (c.retriesAttempted() >= errorCount) {
                LOG.error(message);
            } else if (c.retriesAttempted() >= warnCount) {
                LOG.warn(message);
            } else {
                LOG.info(message);
            }
        }

        private String findExceptionMessage(Throwable t) {
            if (t == null) {
                return null;
            }
            if (t.getMessage() != null) {
                return t.getMessage();
            }
            return findExceptionMessage(t.getCause());
        }
    }
}
