package floq.planner;

public interface IQueryPlanner<T> {
    T planFrom(String sql);
}
