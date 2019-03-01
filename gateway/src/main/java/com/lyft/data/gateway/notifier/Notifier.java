package com.lyft.data.gateway.notifier;

import java.util.List;

public interface Notifier {
  void sendNotification(String subject, String content);

  void sendNotification(String from, List<String> recipients, String subject, String content);
}
