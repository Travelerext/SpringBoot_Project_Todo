package com.example.todo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.todo.mapper.ScheduleMapper;
import com.example.todo.mapper.TodoMapper;
import com.example.todo.model.Frequency;
import com.example.todo.model.Schedule;
import com.example.todo.model.Todo;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.module.ResolutionException;
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
                        .eq(Todo::getUserId, userId)
                        .and(wrapper -> wrapper
                                .or(o -> o
                                        .between(Todo::getDeadline, startOfDay.atZone(ZoneId.systemDefault()).toInstant(), endOfDay.atZone(ZoneId.systemDefault()).toInstant())
                                        .between(Todo::getCreatedAt, startOfDay.atZone(ZoneId.systemDefault()).toInstant(), endOfDay.atZone(ZoneId.systemDefault()).toInstant())
                                )
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
    public Todo createTodo(String content, Long userId, Instant deadline, Frequency frequency, List<Short> customDayOfWeek) throws ResponseStatusException {
        Todo todo = new Todo();
        todo.setContent(content);
        todo.setUserId(userId);
        System.out.println(deadline);
        DeadlineCaltor(deadline, frequency, customDayOfWeek, todo);

        if (frequency != null) {
            Schedule schedule = new Schedule();
            schedule.setFrequency(frequency);
            schedule.setActive(true);

            if (frequency == Frequency.CUSTOM && customDayOfWeek != null) {
                schedule.setCustomDayOfWeek(customDayOfWeek);
            }

            if (scheduleMapper.insert(schedule) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save schedule");
            todo.setScheduleId(schedule.getId());
        }

        if (todoMapper.insert(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save todo");
        return todo;
    }

    @Transactional
    public Todo editTodo(Long todoId, String content, Long userId, Instant deadline, Frequency frequency, List<Short> customDayOfWeek) throws ResponseStatusException {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid todo id");
        if (!Objects.equals(todo.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");

        todo.setContent(content);

        DeadlineCaltor(deadline, frequency, customDayOfWeek, todo);

        if (frequency == null) {
            if (todo.getScheduleId() != null) {
                Schedule schedule = scheduleMapper.selectById(todo.getScheduleId());
                if (schedule == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid schedule id");
                scheduleMapper.deleteById(schedule);
                todo.setScheduleId(null);
            }
            if (todoMapper.updateById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update todo");
            System.out.println("test");
            return todo;
        }

        Schedule schedule;
        if (todo.getScheduleId() != null) {
            schedule = scheduleMapper.selectById(todo.getScheduleId());
            if (schedule == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid schedule id");
        } else schedule = new Schedule();

        schedule.setFrequency(frequency);
        schedule.setActive(true);

        if (frequency == Frequency.CUSTOM && customDayOfWeek != null) {
            schedule.setCustomDayOfWeek(customDayOfWeek);
        }

        if (!scheduleMapper.insertOrUpdate(schedule)) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save schedule");

        todo.setScheduleId(schedule.getId());
        if (todoMapper.updateById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update todo");

        return todo;
    }

    public List<Todo> findTodosByGroup(Long groupId) {
        return todoMapper.selectList(
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getGroupId, groupId)
        );
    }

    private void DeadlineCaltor(Instant deadline, Frequency frequency, List<Short> customDayOfWeek, Todo todo) {
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
    }

    public Todo markDoneTodo(Long todoId, Long userId) throws ResponseStatusException {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid todo id");
        if (!Objects.equals(todo.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");
        todo.setDone(true);
        if (todoMapper.updateById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update todo");
        return todo;
    }

    public void deleteTodo(Long todoId, Long userId) throws ResponseStatusException {
        Todo todo = todoMapper.selectById(todoId);
        if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid todo id");
        if (!Objects.equals(todo.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");
        if (todoMapper.deleteById(todo) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete todo");
    }

    public void cancelSchedule(Long scheduleId, Long userId) throws ResponseStatusException {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid schedule id");
        Todo todo = todoMapper.selectOne(
                new LambdaQueryWrapper<Todo>()
                        .eq(Todo::getScheduleId, schedule.getId())
                        .last("LIMIT 1")
        );
        if (todo == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid schedule id");
        if (!Objects.equals(todo.getUserId(), userId)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user id");
        if (scheduleMapper.updateById(schedule) != 1) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update schedule");
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
