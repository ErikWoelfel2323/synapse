package de.otto.synapse.info;

import de.otto.synapse.channel.ChannelDurationBehind;
import de.otto.synapse.eventsource.EventSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static de.otto.synapse.channel.ChannelDurationBehind.unknown;
import static java.lang.String.format;

public class MessageReceiverEndpointInfoProvider {


    private final MessageReceiverEndpointInfos messageReceiverEndpointInfos = new MessageReceiverEndpointInfos();
    private final Map<String, Instant> channelStartupTimes = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public MessageReceiverEndpointInfoProvider(final Optional<List<EventSource>> eventSources) {
        this(eventSources, Clock.systemDefaultZone());
    }

    /**
     * For testing purposes only.
     *
     * @param eventSources the event sources
     * @param clock clock used to time events end testing the timing behaviour of the provider
     */
    public MessageReceiverEndpointInfoProvider(final Optional<List<EventSource>> eventSources, final Clock clock) {
        eventSources.ifPresent(es -> {
            es.stream()
                    .map(EventSource::getChannelName)
                    .forEach(messageReceiverEndpointInfos::add);
        });
        this.clock = clock;
    }

    @EventListener
    public void on(final MessageReceiverNotification notification) {
        String channelName = notification.getChannelName();
        switch (notification.getStatus()) {
            case STARTING:
                final MessageReceiverEndpointInfo.Builder builder = MessageReceiverEndpointInfo
                        .builder()
                        .withChannelName(notification.getChannelName())
                        .withStatus(MessageReceiverStatus.STARTING)
                        .withMessage(notification.getMessage());
                messageReceiverEndpointInfos.update(channelName, builder.build());
                channelStartupTimes.put(channelName, clock.instant());
                break;
            case STARTED:
                messageReceiverEndpointInfos.update(channelName, MessageReceiverEndpointInfo
                        .builder()
                        .withChannelName(channelName)
                        .withStatus(notification.getStatus())
                        .withMessage(notification.getMessage())
                        .build());
                break;
            case RUNNING:
                final ChannelDurationBehind durationBehind = notification.getChannelDurationBehind().orElse(unknown());
                final MessageReceiverEndpointInfo endpointInfo = MessageReceiverEndpointInfo
                        .builder()
                        .withChannelName(channelName)
                        .withStatus(notification.getStatus())
                        .withChannelDurationBehind(durationBehind)
                        .withMessage(format("Channel is %s behind head.", durationBehind))
                        .build();
                messageReceiverEndpointInfos.update(channelName, endpointInfo);
                break;
            case FINISHED:
                final Duration runtime = Duration.between(channelStartupTimes.get(channelName), clock.instant());
                ChannelDurationBehind lastDurationBehind = messageReceiverEndpointInfos.getChannelInfoFor(channelName).getDurationBehind().orElse(null);
                messageReceiverEndpointInfos.update(channelName, MessageReceiverEndpointInfo
                        .builder()
                        .withChannelName(channelName)
                        .withStatus(notification.getStatus())
                        .withChannelDurationBehind(lastDurationBehind)
                        .withMessage(format("%s Finished consumption after %s.", notification.getMessage(), runtime))
                        .build());
                break;
            case FAILED:
                messageReceiverEndpointInfos.update(channelName, MessageReceiverEndpointInfo
                        .builder()
                        .withChannelName(channelName)
                        .withStatus(notification.getStatus())
                        .withMessage(notification.getMessage())
                        .build());
                break;
            default:
                break;
        }
    }

    public MessageReceiverEndpointInfos getInfos() {
        return messageReceiverEndpointInfos;
    }

}
