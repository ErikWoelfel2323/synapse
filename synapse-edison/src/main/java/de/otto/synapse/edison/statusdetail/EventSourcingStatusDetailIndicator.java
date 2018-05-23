package de.otto.synapse.edison.statusdetail;

import de.otto.edison.status.domain.Status;
import de.otto.edison.status.domain.StatusDetail;
import de.otto.edison.status.indicator.StatusDetailIndicator;
import de.otto.synapse.edison.provider.MessageReceiverEndpointInfoProvider;
import de.otto.synapse.info.MessageEndpointStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static de.otto.synapse.info.MessageEndpointStatus.FAILED;

@Component
public class EventSourcingStatusDetailIndicator implements StatusDetailIndicator {

    private final MessageReceiverEndpointInfoProvider provider;

    @Autowired
    public EventSourcingStatusDetailIndicator(final MessageReceiverEndpointInfoProvider provider) {
        this.provider = provider;
    }

    @Override
    public StatusDetail statusDetail() {
        return null;
    }

    @Override
    public List<StatusDetail> statusDetails() {
        return provider.getInfos().stream()
                .map(channelInfo -> createStatusDetail(statusOf(channelInfo.getStatus()), channelInfo.getChannelName(), channelInfo.getMessage()))
                .collect(Collectors.toList());
    }

    private Status statusOf(final MessageEndpointStatus status) {
        return status != FAILED ? Status.OK : Status.ERROR;
    }

    private StatusDetail createStatusDetail(Status status, String name, String message) {
        return StatusDetail.statusDetail(name, status, message);
    }

}
