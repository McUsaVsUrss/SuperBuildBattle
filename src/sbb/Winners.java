package sbb;


public class Winners implements Comparable<Winners> {
    SbbPlayer team;
    int score;

    public Winners(SbbPlayer name, int score) {
        this.team = name;
        this.score = score;
    }

    public int compareTo(Winners user) {
        return user.getScore() - this.getScore();
    }

    public SbbPlayer getName() {
        return team;
    }

    public int getScore() {
        return score;
    }

    public String toString() {
        return "Name: " + team + " Score: " + score;
    }
}
