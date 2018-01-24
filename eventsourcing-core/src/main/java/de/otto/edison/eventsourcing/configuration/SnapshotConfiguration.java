package de.otto.edison.eventsourcing.configuration;

import de.otto.edison.aws.configuration.AwsConfiguration;
import de.otto.edison.aws.s3.S3Service;
import de.otto.edison.aws.s3.configuration.S3Configuration;
import de.otto.edison.eventsourcing.s3.SnapshotConsumerService;
import de.otto.edison.eventsourcing.s3.SnapshotReadService;
import de.otto.edison.eventsourcing.s3.SnapshotWriteService;
import de.otto.edison.eventsourcing.s3.TempFileService;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EventSourcingProperties.class)
@ImportAutoConfiguration({
        AwsConfiguration.class,
        S3Configuration.class,
        EventSourcingBootstrapConfiguration.class
})
public class SnapshotConfiguration {


    @Bean
    public TempFileService tempFileService() {
        return new TempFileService();
    }

    @Bean
    @ConditionalOnMissingBean
    public SnapshotReadService snapshotService(final S3Service s3Service,
                                               final EventSourcingProperties eventSourcingProperties,
                                               final TempFileService tempFileService) {
        return new SnapshotReadService(eventSourcingProperties, s3Service, tempFileService);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnapshotWriteService snapshotCreationService(final S3Service s3Service,
                                                        final EventSourcingProperties eventSourcingProperties) {
        return new SnapshotWriteService(s3Service, eventSourcingProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SnapshotConsumerService snapshotConsumerService() {
        return new SnapshotConsumerService();
    }
}
