package hansung.arcade.outgame.lobby;

public record Room(int number, String title, int players, int capacity, String map, Status status) {
    public enum Status {
        WAITING("대기중"), PLAYING("게임중");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public boolean canJoin() {
        return status == Status.WAITING && players < capacity;
    }
}
