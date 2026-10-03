package com.learning.spring.ioc;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/*
 * ============================================================================
 *  TOPIC 01 : RESOLVING MULTIPLE CANDIDATES - @Primary, @Qualifier, List, Map
 * ============================================================================
 *
 *  If TWO beans implement NotificationSender and you ask for "a NotificationSender",
 *  Spring fails at startup with NoUniqueBeanDefinitionException. Ways to choose:
 *
 *    @Primary            -> the DEFAULT winner when nothing more specific is asked for
 *    @Qualifier("sms")   -> pick a specific bean by qualifier/name at the injection point
 *    List<NotificationSender>        -> inject ALL implementations (ordered by @Order)
 *    Map<String, NotificationSender> -> ALL of them keyed by BEAN NAME (strategy lookup!)
 *
 *  Stereotype annotations (all are @Component underneath):
 *    @Component  generic bean
 *    @Service    business logic (no extra behaviour, documents intent)
 *    @Repository persistence; ALSO translates DB exceptions into Spring's
 *                DataAccessException hierarchy
 *    @Controller / @RestController  web layer
 */
public final class Notifications {

    private Notifications() {}

    public interface NotificationSender {
        String send(String to, String message);
    }

    @Component("email")
    @Primary                                   // default choice
    public static class EmailSender implements NotificationSender {
        public String send(String to, String message) { return "EMAIL to " + to + ": " + message; }
    }

    @Component("sms")
    public static class SmsSender implements NotificationSender {
        public String send(String to, String message) { return "SMS to " + to + ": " + message; }
    }

    @Component("push")
    public static class PushSender implements NotificationSender {
        public String send(String to, String message) { return "PUSH to " + to + ": " + message; }
    }

    @Service
    public static class NotificationService {
        private final NotificationSender defaultSender;           // gets @Primary (email)
        private final NotificationSender smsSender;               // explicitly "sms"
        private final List<NotificationSender> allSenders;        // all 3
        private final Map<String, NotificationSender> byName;     // {email=..., sms=..., push=...}

        public NotificationService(NotificationSender defaultSender,
                                   @Qualifier("sms") NotificationSender smsSender,
                                   List<NotificationSender> allSenders,
                                   Map<String, NotificationSender> byName) {
            this.defaultSender = defaultSender;
            this.smsSender = smsSender;
            this.allSenders = allSenders;
            this.byName = byName;
        }

        public String notifyDefault(String to, String msg) { return defaultSender.send(to, msg); }
        public String notifyBySms(String to, String msg) { return smsSender.send(to, msg); }
        public int channelCount() { return allSenders.size(); }

        // Strategy pattern for free: choose the implementation at RUNTIME by name
        public String notifyVia(String channel, String to, String msg) {
            NotificationSender sender = byName.get(channel);
            if (sender == null) throw new IllegalArgumentException("Unknown channel " + channel + ", known: " + byName.keySet());
            return sender.send(to, msg);
        }
    }
}
