package it.auties.whatsapp4j.protobuf.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.auties.whatsapp4j.listener.WhatsappListener;
import it.auties.whatsapp4j.protobuf.contact.Contact;
import it.auties.whatsapp4j.protobuf.contact.ContactStatus;
import it.auties.whatsapp4j.protobuf.info.MessageInfo;
import it.auties.whatsapp4j.protobuf.model.Messages;
import it.auties.whatsapp4j.protobuf.model.Node;
import it.auties.whatsapp4j.response.model.json.JsonResponse;
import it.auties.whatsapp4j.utils.WhatsappUtils;
import it.auties.whatsapp4j.api.WhatsappAPI;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * A model class that represents a Chat.
 * A chat can be of two types: a conversation with a contact or a group.
 * To check if this chat is a group use {@link Chat#isGroup()} or {@link WhatsappUtils#isGroup(String)}.
 * This class is only a model, this means that changing its values will have no real effect on WhatsappWeb's servers.
 * Instead, methods inside {@link WhatsappAPI} should be used.
 * This class also offers a builder, accessible using {@link Chat#builder()}.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Accessors(fluent = true)
public class Chat {
  /**
   * The non null unique jid used to identify this chat
   */
  @JsonProperty(value = "2")
  private @NonNull String jid;
  
  /**
   * The non null display name of this chat
   */
  @JsonProperty(value = "1")
  private @NonNull String displayName;

  /**
   * A non null arrayList of messages in this chat sorted chronologically
   */
  @Builder.Default
  private @NonNull Messages messages = new Messages();

  /**
   * A map that holds the status of each participant, excluding yourself, for this chat.
   * If the chat is not a group, this map's size will range from 0 to 1.
   * Otherwise, it will range from 0 to the number of participants - 1.
   * It is important to remember that is not guaranteed that every participant will be present as a key.
   * In this case, if this chat is a group, it can be safely assumed that the user is not available.
   * Otherwise, it's recommended to use {@link it.auties.whatsapp4j.api.WhatsappAPI#subscribeToContactPresence(Contact)} to force Whatsapp to send updates regarding the status of the other participant.
   * It's also possible to listen for updates to a contact's presence in a group or in a conversation by implementing {@link WhatsappListener#onContactPresenceUpdate}.
   * The presence that this map indicates might not line up with {@link Contact#lastKnownPresence()} if the contact is composing, recording or paused.
   * This is because a contact can be online on Whatsapp and composing, recording or paused in a specific chat.
   */
  @Builder.Default
  private @NonNull Map<Contact, ContactStatus> presences = new HashMap<>();

  /**
   * The non null mute of this chat
   */
  @Builder.Default
  private @NonNull ChatMute mute = ChatMute.UNKNOWN;

  /**
   * The nullable new unique jid for this Chat.
   * This field is not null when a contact changes phone number and connects their new phone number with Whatsapp.
   */
  private String newJid;

  /**
   * The time in seconds since {@link java.time.Instant#EPOCH} for the latest message in {@link Chat#messages}
   */
  private long timestamp;

  /**
   * The number of unread messages in this chat.
   * If this field is negative, this chat is marked as unread.
   */
  private int unreadMessages;

  /**
   * The time in seconds since {@link java.time.Instant#EPOCH} when this chat was pinned to the top.
   * If the chat isn't pinned, this field has a value of 0.
   */
  private long pinned;

  /**
   * The time in seconds before a message is automatically deleted from this chat both locally and from WhatsappWeb's servers.
   * If ephemeral messages aren't enabled, this field has a value of 0
   */
  private long ephemeralMessageDuration;

  /**
   * The time in seconds since {@link java.time.Instant#EPOCH} when ephemeral messages were turned on.
   * If ephemeral messages aren't enabled, this field has a value of 0.
   */
  private long ephemeralMessagesToggleTime;

  /**
   * This field is used to determine whether a chat is archived or not.
   */
  private boolean isArchived;

  /**
   * This field is used to determine whether a chat is read only or not.
   * If true, it means that it's not possible to send messages here.
   * This is the case, for example, for groups where only admins can send messages.
   */
  private boolean isReadOnly;

  /**
   * This field is used to determine whether a chat was marked as being spam or not.
   */
  private boolean isSpam;

  /**
   * Constructs a new Chat from a map of attributes.
   * This method is usually used to deserialize a Chat from the attributes of a {@link Node}.
   *
   * @return a new instance of Chat
   */
  public static @NonNull Chat fromAttributes(@NonNull Map<String, String> attrs) {
    var json = JsonResponse.fromMap(attrs);
    var jid = json.getString("jid")
            .orElseThrow(() -> new IllegalArgumentException("Cannot parse chat(%s): missing attribute jid".formatted(json)));
    return Chat.builder()
            .displayName(json.getString("name").orElse(WhatsappUtils.phoneNumberFromJid(jid)))
            .jid(jid)
            .timestamp(json.getLong("t").orElse(ZonedDateTime.now().toEpochSecond()))
            .newJid(json.getString("new_jid").orElse(null))
            .unreadMessages(json.getInteger("count").orElse(0))
            .mute(json.getInteger("mute").map(ChatMute::new).orElse(ChatMute.UNKNOWN))
            .isSpam(json.getBoolean("spam"))
            .isArchived(json.getBoolean(("archive")))
            .isReadOnly(json.getBoolean(("read_only")))
            .pinned(json.getLong("pin").orElse(0L))
            .build();
  }

  /**
   * Returns a boolean to represent whether this chat is a group or not
   *
   * @return true if this chat is a group
   */
  public boolean isGroup() {
    return WhatsappUtils.isGroup(jid);
  }

  /**
   * Returns a boolean to represent whether this chat is pinned or not
   *
   * @return true if this chat is pinned
   */
  public boolean isPinned() {
    return pinned != 0;
  }

  /**
   * Returns a boolean to represent whether ephemeral messages are enabled for this chat
   *
   * @return true if ephemeral messages are enabled for this chat
   */
  public boolean isEphemeral() {
    return ephemeralMessageDuration != 0 && ephemeralMessagesToggleTime != 0;
  }

  /**
   * Returns a boolean to represent whether this chat has a new jid
   *
   * @return true if this chat has a new jid
   */
  public boolean hasNewJid() {
    return newJid != null;
  }

  /**
   * Returns a boolean to represent whether this chat has unread messages
   *
   * @return true if this chat has unread messages
   */
  public boolean hasUnreadMessages() {
    return !isMarkedRead();
  }

  /**
   * Returns a boolean to represent whether this chat is marked as read
   *
   * @return true if this chat is marked as read
   */
  public boolean isMarkedRead() {
    return unreadMessages == 0;
  }

  /**
   * Returns a boolean to represent whether this chat was manually marked as unread
   *
   * @return true if this chat is marked as unread
   */
  public boolean isManuallyMarkedUnread() {
    return unreadMessages < 0;
  }

  /**
   * Returns an optional value containing the new jid of this chat
   *
   * @return a non empty optional if the new jid is not null, otherwise an empty optional
   */
  public @NonNull Optional<String> newJid() {
    return Optional.ofNullable(newJid);
  }

  /**
   * Returns an optional value containing the time this chat was pinned
   *
   * @return a non empty optional if the chat is pinned, otherwise an empty optional
   */
  public @NonNull Optional<ZonedDateTime> pinned() {
    return WhatsappUtils.parseWhatsappTime(pinned);
  }

  /**
   * Returns an optional value containing the time in seconds before a message is automatically deleted from this chat both locally and from WhatsappWeb's servers
   *
   * @return a non empty optional if ephemeral messages are enabled for this chat, otherwise an empty optional
   */
  public @NonNull Optional<ZonedDateTime> ephemeralMessageDuration() {
    return WhatsappUtils.parseWhatsappTime(ephemeralMessageDuration);
  }

  /**
   * Returns an optional value containing the time in seconds since {@link java.time.Instant#EPOCH} when ephemeral messages were turned on
   *
   * @return a non empty optional if ephemeral messages are enabled for this chat, otherwise an empty optional
   */
  public @NonNull Optional<ZonedDateTime> ephemeralMessagesToggleTime() {
    return WhatsappUtils.parseWhatsappTime(ephemeralMessagesToggleTime);
  }

  /**
   * Returns an optional value containing the latest message in chronological terms for this chat
   *
   * @return a non empty optional if {@link Chat#messages} isn't empty, otherwise an empty optional
   */
  public @NonNull Optional<MessageInfo> lastMessage() {
    return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(messages.size() - 1));
  }

  /**
   * Returns an optional value containing the first message in chronological terms for this chat
   *
   * @return a non empty optional if {@link Chat#messages} isn't empty, otherwise an empty optional
   */
  public @NonNull Optional<MessageInfo> firstMessage() {
    return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
  }
}
