package com.rentease.service;

import com.rentease.entity.SmsLog;
import com.rentease.entity.SmsStatus;
import com.rentease.entity.SmsType;
import com.rentease.entity.User;
import com.rentease.integration.SemaphoreClient;
import com.rentease.repository.SmsLogRepository;
import com.rentease.util.PhoneNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Sends an SMS through Semaphore and records the result in the SMS log (FR055, FR056). */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SemaphoreClient semaphore;
    private final SmsLogRepository logs;

    public SmsService(SemaphoreClient semaphore, SmsLogRepository logs) {
        this.semaphore = semaphore;
        this.logs = logs;
    }

    /**
     * @param body    the text that is sent
     * @param logBody the text that is stored in the log (use this to leave out secrets such as passwords)
     * @return true if Semaphore accepted the message
     */
    public boolean send(User recipient, SmsType type, String mobileNumber, String body, String logBody) {
        String number = PhoneNumbers.toInternational(mobileNumber);

        SmsLog entry = new SmsLog();
        entry.setUser(recipient);
        entry.setMessageType(type);
        entry.setRecipientNumber(number);
        entry.setMessageBody(truncate(logBody, 500));

        boolean sent = false;
        if (semaphore.isConfigured()) {
            SemaphoreClient.Result result = semaphore.send(number, body);
            entry.setAttemptCount(1);
            sent = result.success();
            entry.setStatus(sent ? SmsStatus.SENT : SmsStatus.FAILED);
            entry.setFailureReason(sent ? null : truncate(result.detail(), 255));
        } else {
            log.warn("SMS is not configured (DEV MODE): a {} message to {} was not sent.", type, number);
            entry.setAttemptCount(0);
            entry.setStatus(SmsStatus.FAILED);
            entry.setFailureReason("SMS is not configured (set SEMAPHORE_API_KEY)");
        }
        logs.save(entry);
        return sent;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
