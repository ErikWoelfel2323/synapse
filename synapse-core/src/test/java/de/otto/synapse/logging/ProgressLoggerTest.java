package de.otto.synapse.logging;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.Marker;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ProgressLoggerTest {

    private Logger logger;

    @Before
    public void setUp() {
        logger = mock(Logger.class);
    }

    @Test
    public void shouldLogAllPercentageSteps() {
        // given
        final int expectedCount = 1000;
        ProgressLogger progressLogger = new ProgressLogger(logger, expectedCount, null);

        // when
        for(int i = 0; i <= expectedCount; i++){
            progressLogger.incrementAndLog();
        }

        // then
        verify(logger, times(100)).info((Marker)isNull(), anyString(), anyLong());
    }

    @Test
    public void shouldLogProcessedText(){
        // given
        final int expectedCount = 100;
        ProgressLogger progressLogger = new ProgressLogger(logger, expectedCount, null);

        // when
        progressLogger.incrementAndLog();

        // then
        verify(logger, times(1)).info((Marker)isNull(), eq("processed {}% of entries"), eq(1L));
    }
}