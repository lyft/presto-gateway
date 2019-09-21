package com.lyft.data.gateway.ha.notifier;

import com.lyft.data.gateway.ha.config.NotifierConfiguration;
import java.util.List;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EmailNotifier implements Notifier {
  private final NotifierConfiguration notifierConfiguration;
  private final Properties props;

  public EmailNotifier(NotifierConfiguration notifierConfiguration) {
    this.notifierConfiguration = notifierConfiguration;
    this.props = System.getProperties();
    this.props.put("mail.smtp.starttls.enable", notifierConfiguration.isStartTlsEnabled());
    this.props.put("mail.smtp.auth", notifierConfiguration.isSmtpAuthEnabled());
    this.props.put("mail.smtp.port", notifierConfiguration.getSmtpPort());

    if (notifierConfiguration.getSmtpHost() != null) {
      this.props.put("mail.smtp.host", notifierConfiguration.getSmtpHost());
    }
    if (notifierConfiguration.getSmtpUser() != null) {
      this.props.put("mail.smtp.user", notifierConfiguration.getSmtpUser());
    }
    if (notifierConfiguration.getSmtpPassword() != null) {
      this.props.put("mail.smtp.password", notifierConfiguration.getSmtpPassword());
    }
  }

  @Override
  public void sendNotification(String subject, String content) {
    sendNotification(
        notifierConfiguration.getSender(),
        notifierConfiguration.getRecipients(),
        "Presto Error: " + subject,
        content);
  }

  @Override
  public void sendNotification(
      String from, List<String> recipients, String subject, String content) {
    if (recipients.size() > 0) {
      Session session = Session.getDefaultInstance(props);
      MimeMessage message = new MimeMessage(session);
      try {
        message.setFrom(new InternetAddress(from));

        // To get the array of addresses
        recipients.forEach(
            r -> {
              try {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(r));
              } catch (Exception e) {
                log.error("Recipient email [" + e + "] could not be added", e);
              }
            });
        message.setSubject(subject);
        message.setText(content);
        try (Transport transport = session.getTransport("smtp")) {
          if (notifierConfiguration.isSmtpAuthEnabled()) {
            transport.connect(
                notifierConfiguration.getSmtpHost(),
                notifierConfiguration.getSmtpUser(),
                notifierConfiguration.getSmtpPassword());
          } else {
            transport.connect();
          }
          transport.sendMessage(message, message.getAllRecipients());
        } catch (Exception e) {
          log.error("Error creating email transport client", e);
        }
        log.debug("Sent message [{}] successfully.", content);
      } catch (Exception e) {
        log.error("Error sending alert", e);
      }
    } else {
      log.warn("No recipients configured to send app notification [{}]", content);
    }
  }
}
