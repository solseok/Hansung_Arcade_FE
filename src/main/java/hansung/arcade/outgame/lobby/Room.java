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

    /** 한 명이 바로 들어갈 수 있는지 */
    public boolean canJoin() {
        return hasSeats(1);
    }

    /** seats명이 함께 바로 들어갈 수 있는지: 대기 중이고 자리가 남았다 */
    public boolean hasSeats(int seats) {
        return status == Status.WAITING && players + seats <= capacity;
    }
}
