package domain.model;

public enum MaturityGrid {

    GRID_53(53, 1.0 / 52.0);   // 53 weekly steps ≈ 1-year horizon

    private final int steps;
    private final double dt;    // time increment in years

    MaturityGrid(int steps, double dt) {
        this.steps = steps;
        this.dt = dt;
    }

    public int getSteps() { return steps; }
    public double getDt()  { return dt; }
    public double getTotalTime() { return steps * dt; }
}

