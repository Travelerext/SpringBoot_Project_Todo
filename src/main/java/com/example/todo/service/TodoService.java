package com.example.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.todo.mapper.ScheduleMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Frequency;
import com.example.todo.model.Schedule;
import com.example.todo.model.Todo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Objects;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final ScheduleMapper scheduleMapper;

    public TodoService(TodoMapper todoMapper, ScheduleMapper scheduleMapper) {
        this.todoMapper = todoMapper;
        this.scheduleMapper = scheduleMapper;
    }

    public Page<Todo> getTodayTodos(Integer current, Integer size, Long userId) {
        Page<Todo> page = new Page<>(current, size);
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59);

        return todoMapper.selectPage(
                page,
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getUserId, userId)
                        .or(wrapper -> wrapper
                                .and(o -> o
                                        .ge(Todo::getDeadline, startOfDay.atZone(ZoneId.systemDefault()).toInstant())
                                        .le(Todo::getDeadline, endOfDay.atZone(ZoneId.systemDefault()).toInstant()))
                                .and(o -> o
                                        .ge(Todo::getCreatedAt, startOfDay.atZone(ZoneId.systemDefault()).toInstant())
                                        .le(Todo::getCreatedAt, endOfDay.atZone(ZoneId.systemDefault()).toInstant()))
                        )
                        .orderByAsc(Todo::getDeadline)
        );
    }

    public Page<Todo> getAllUndoneTodos(Integer current, Integer size, Long userId) {
        Page<Todo> page = new Page<>(current, size);
        return todoMapper.selectPage(
                page,
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getUserId, userId)
                        .eq(Todo::getDone, false)
                        .lt(Todo::getDeadline, Instant.now())
                        .orderByAsc(Todo::getDeadline)
        );
    }

    public Page<Todo> getOverdueTodos(Integer current, Integer size, Long userId) {
        Page<Todo> page = new Page<>(current, size);
        return todoMapper.selectPage(
                page,
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getUserId, userId)
                        .eq(Todo::getDone, false)
                        .gt(Todo::getDeadline, Instant.now())
                        .orderByDesc(Todo::getCreatedAt)
        );
    }

    public Page<Todo> getAllDoneTodos(Integer current, Integer size, Long userId) {
        Page<Todo> page = new Page<>(current, size);
        return todoMapper.selectPage(
                page,
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getUserId, userId)
                        .eq(Todo::getDone, true)
                        .orderByDesc(Todo::getCreatedAt)
        );
    }

    public Schedule getScheduleByTodo(Todo todo) {
        if (todo.getScheduleId() != null) {
            return scheduleMapper.selectById(todo.getScheduleId());
        }
        return null;
    }

    @Transactional
    public Boolean createTodo(String content, Long userId, Instant deadline, Frequency frequency, List<Short> customDayOfWeek) {
        Todo todo = new Todo();
        todo.setContent(content);
        todo.setUserId(userId);

        if (deadline == null && frequency != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadlineDateTime = null;

            switch (frequency) {
                case DAILY: {
                    deadlineDateTime = now.withHour(23).withMinute(59).withSecond(59);
                    break;
                }
                case WEEKLY: {
                    int daysUntilSunday = 7 - now.getDayOfWeek().getValue();
                    deadlineDateTime = now.plusDays(daysUntilSunday).withHour(23).withMinute(59).withSecond(59);
                    break;
                }
                case MONTHLY: {
                    deadlineDateTime = now.withDayOfMonth(
                                    now.getMonth().length(now.toLocalDate().isLeapYear()))
                            .withHour(23).withMinute(59).withSecond(59);
                    break;
                }
                case CUSTOM: {
                    if (customDayOfWeek != null && !customDayOfWeek.isEmpty()) {
                        int daysUntilSunday = 7 - customDayOfWeek.getFirst();
                        deadlineDateTime = now.plusDays(daysUntilSunday);
                    }
                }
            }

            if (deadlineDateTime != null) {
                deadline = deadlineDateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
        }

        todo.setDeadline(deadline);

        if (frequency == null) {
            return todoMapper.insert(todo) == 1;
        } else {
            Schedule schedule = new Schedule();
            schedule.setFrequency(frequency);
            schedule.setActive(true);

            if (frequency == Frequency.CUSTOM && customDayOfWeek != null) {
                schedule.setCustomDayOfWeek(customDayOfWeek);
            }

            boolean scheduleInsert = scheduleMapper.insert(schedule) == 1;

            if (scheduleInsert) {
                todo.setScheduleId(schedule.getId());
                return todoMapper.insert(todo) == 1;
            }

            return false;
        }
    }

    @Transactional
    public Boolean editTodo(Long todoId, String content, Long userId, Instant deadline, Frequency frequency, List<Short> customDayOfWeek) {
        Todo todo = todoMapper.selectById(todoId);

        if (todo == null || !Objects.equals(todo.getUserId(), userId)) return false;

        todo.setContent(content);

        if (deadline == null && frequency != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime deadlineDateTime = null;

            switch (frequency) {
                case DAILY: {
                    deadlineDateTime = now.withHour(23).withMinute(59).withSecond(59);
                    break;
                }
                case WEEKLY: {
                    int daysUntilSunday = 7 - now.getDayOfWeek().getValue();
                    deadlineDateTime = now.plusDays(daysUntilSunday).withHour(23).withMinute(59).withSecond(59);
                    break;
                }
                case MONTHLY: {
                    deadlineDateTime = now.withDayOfMonth(now.getMonth().length(now.toLocalDate().isLeapYear()))
                            .withHour(23).withMinute(59).withSecond(59);
                    break;
                }
                case CUSTOM: {
                    if (customDayOfWeek != null && !customDayOfWeek.isEmpty()) {
                        int daysUntilSunday = 7 - customDayOfWeek.getFirst();
                        deadlineDateTime = now.plusDays(daysUntilSunday);
                    }
                }
            }

            if (deadlineDateTime != null) {
                deadline = deadlineDateTime.atZone(ZoneId.systemDefault()).toInstant();
            }
        }

        todo.setDeadline(deadline);

        if (frequency == null) {
            return todoMapper.updateById(todo) == 1;
        }

        Schedule schedule;
        if (todo.getScheduleId() != null) {
            schedule = scheduleMapper.selectById(todo.getScheduleId());
            if (schedule == null) return false;
        } else schedule = new Schedule();

        schedule.setFrequency(frequency);
        schedule.setActive(true);

        if (frequency == Frequency.CUSTOM && customDayOfWeek != null) {
            schedule.setCustomDayOfWeek(customDayOfWeek);
        }

        boolean scheduleInsert = scheduleMapper.insertOrUpdate(schedule);

        if (scheduleInsert) {
            todo.setScheduleId(schedule.getId());
            return todoMapper.updateById(todo) == 1;
        }
        return false;
    }

    public Boolean markDoneTodo(Long todoId) {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) {
            return false;
        }
        todo.setDone(true);
        return todoMapper.updateById(todo) == 1;
    }

    public Boolean deleteTodo(Long todoId) {
        return todoMapper.deleteById(todoId) == 1;
    }

    public Boolean cancelSchedule(Long scheduleId) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            return false;
        }
        return scheduleMapper.updateById(schedule) == 1;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void createDailyPeriodicTodos() {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getActive, true)
                   .eq(Schedule::getFrequency, Frequency.DAILY);
        List<Schedule> activeSchedules = scheduleMapper.selectList(queryWrapper);

        for (Schedule schedule : activeSchedules) {
            createDailyTodos(schedule);
        }
    }

    @Scheduled(cron = "0 0 0 ? * MON")
    @Transactional
    public void createWeeklyPeriodicTodos() {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getActive, true)
                   .eq(Schedule::getFrequency, Frequency.WEEKLY);
        List<Schedule> activeSchedules = scheduleMapper.selectList(queryWrapper);

        for (Schedule schedule : activeSchedules) {
            createWeeklyTodos(schedule);
        }
    }

    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void createMonthlyPeriodicTodos() {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getActive, true)
                   .eq(Schedule::getFrequency, Frequency.MONTHLY);
        List<Schedule> activeSchedules = scheduleMapper.selectList(queryWrapper);

        for (Schedule schedule : activeSchedules) {
            createMonthlyTodos(schedule);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void createCustomPeriodicTodos() {
        LambdaQueryWrapper<Schedule> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Schedule::getActive, true)
                   .eq(Schedule::getFrequency, Frequency.CUSTOM);
        List<Schedule> activeSchedules = scheduleMapper.selectList(queryWrapper);

        LocalDate today = LocalDate.now();
        short dayOfWeek = (short) today.getDayOfWeek().getValue();

        for (Schedule schedule : activeSchedules) {
            if (schedule.getCustomDayOfWeek() != null && 
                schedule.getCustomDayOfWeek().contains(dayOfWeek)) {
                createDailyTodos(schedule);
            }
        }
    }

    private void createDailyTodos(Schedule schedule) {
        Todo originalTodo = todoMapper.selectOne(
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getScheduleId, schedule.getId())
                        .orderByDesc(Todo::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (originalTodo != null) {
            Todo newTodo = new Todo();
            newTodo.setContent(originalTodo.getContent());
            newTodo.setUserId(originalTodo.getUserId());
            newTodo.setScheduleId(schedule.getId());
            newTodo.setGroupId(originalTodo.getGroupId());

            if (originalTodo.getDeadline() != null) {
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        originalTodo.getDeadline(), ZoneId.systemDefault());
                LocalDateTime newDateTime = dateTime.plusDays(1);
                newTodo.setDeadline(newDateTime.atZone(ZoneId.systemDefault()).toInstant());
            }

            todoMapper.insert(newTodo);
        }
    }

    private void createWeeklyTodos(Schedule schedule) {
        Todo originalTodo = todoMapper.selectOne(
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getScheduleId, schedule.getId())
                        .orderByDesc(Todo::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (originalTodo != null) {
            Todo newTodo = new Todo();
            newTodo.setContent(originalTodo.getContent());
            newTodo.setUserId(originalTodo.getUserId());
            newTodo.setScheduleId(schedule.getId());
            newTodo.setGroupId(originalTodo.getGroupId());

            if (originalTodo.getDeadline() != null) {
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        originalTodo.getDeadline(), ZoneId.systemDefault());
                LocalDateTime newDateTime = dateTime.plusWeeks(1);
                newTodo.setDeadline(newDateTime.atZone(ZoneId.systemDefault()).toInstant());
            }

            todoMapper.insert(newTodo);
        }
    }

    private void createMonthlyTodos(Schedule schedule) {
        Todo originalTodo = todoMapper.selectOne(
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getScheduleId, schedule.getId())
                        .orderByDesc(Todo::getCreatedAt)
                        .last("LIMIT 1")
        );

        if (originalTodo != null) {
            Todo newTodo = new Todo();
            newTodo.setContent(originalTodo.getContent());
            newTodo.setUserId(originalTodo.getUserId());
            newTodo.setScheduleId(schedule.getId());
            newTodo.setGroupId(originalTodo.getGroupId());

            if (originalTodo.getDeadline() != null) {
                LocalDateTime dateTime = LocalDateTime.ofInstant(
                        originalTodo.getDeadline(), ZoneId.systemDefault());
                LocalDateTime newDateTime = dateTime.plusMonths(1);
                newTodo.setDeadline(newDateTime.atZone(ZoneId.systemDefault()).toInstant());
            }

            todoMapper.insert(newTodo);
        }
    }
}
