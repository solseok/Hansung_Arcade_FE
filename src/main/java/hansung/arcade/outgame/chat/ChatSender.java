package hansung.arcade.outgame.chat;

@FunctionalInterface
public interface ChatSender {
    void send(String message);
}
