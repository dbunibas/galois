package bsf.planner;

public interface IQueryPlanner<T> {
    T planFrom(String sql);
}
