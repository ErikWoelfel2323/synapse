package de.otto.synapse.channel;

import de.otto.synapse.endpoint.MessageInterceptorRegistry;
import de.otto.synapse.info.MessageReceiverNotification;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.concurrent.ExecutionException;

import static de.otto.synapse.channel.ChannelPosition.fromHorizon;
import static de.otto.synapse.channel.StopCondition.endOfChannel;
import static de.otto.synapse.info.MessageReceiverStatus.*;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class InMemoryChannelTest {
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    @Test
    public void shouldPublishStartedAndFinishedEvents() throws ExecutionException, InterruptedException {
        // given
        InMemoryChannel inMemoryChannel = new InMemoryChannel("some-stream", new MessageInterceptorRegistry(), eventPublisher);

        // when
        inMemoryChannel.consumeUntil(fromHorizon(), endOfChannel()).get();

        // then
        ArgumentCaptor<MessageReceiverNotification> notificationArgumentCaptor = ArgumentCaptor.forClass(MessageReceiverNotification.class);
        verify(eventPublisher, times(3)).publishEvent(notificationArgumentCaptor.capture());

        MessageReceiverNotification startingNotification = notificationArgumentCaptor.getAllValues().get(0);
        assertThat(startingNotification.getStatus(), is(STARTING));
        assertThat(startingNotification.getChannelDurationBehind().isPresent(), is(false));
        assertThat(startingNotification.getChannelName(), is("some-stream"));

        MessageReceiverNotification startedNotification = notificationArgumentCaptor.getAllValues().get(1);
        assertThat(startedNotification.getStatus(), is(STARTED));
        assertThat(startedNotification.getChannelDurationBehind().isPresent(), is(false));
        assertThat(startedNotification.getChannelName(), is("some-stream"));

        MessageReceiverNotification finishedNotification = notificationArgumentCaptor.getAllValues().get(2);
        assertThat(finishedNotification.getStatus(), is(FINISHED));
        assertThat(finishedNotification.getChannelDurationBehind().isPresent(), is(false));
        assertThat(finishedNotification.getChannelName(), is("some-stream"));
    }
}