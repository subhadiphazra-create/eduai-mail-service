package com.eduai.mailservice.service.mail;

import com.eduai.mailservice.dto.request.BulkEmailRequestDto;
import com.eduai.mailservice.dto.response.BulkEmailResponseDto;

import java.util.Map;

/**
 * Queue management service contract.
 */
public interface QueueService {

    /**
     * Enqueue a bulk email batch.
     *
     * @param request bulk email request
     * @return batch response
     */
    BulkEmailResponseDto enqueueBulk(BulkEmailRequestDto request);

    /**
     * Get current queue depths by queue name.
     *
     * @return map of queueKey → depth
     */
    Map<String, Long> getQueueDepths();
}
