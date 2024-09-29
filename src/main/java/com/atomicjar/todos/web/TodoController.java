package com.atomicjar.todos.web;

import com.atomicjar.todos.entity.Todo;
import com.atomicjar.todos.repository.TodoRepository;
import com.atomicjar.todos.service.Estimation;
import com.atomicjar.todos.service.TodoEstimator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoEstimator ai;
    private final TodoRepository repository;

    public TodoController(TodoRepository repository, TodoEstimator ai) {
        this.ai = ai;
        this.repository = repository;
    }

    @GetMapping
    public Iterable<Todo> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Todo> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    @PostMapping
    public ResponseEntity<Todo> save(@Valid @RequestBody Todo todo) {
        todo.setId(null);
        Estimation estimate = ai.chat("todo title is '" + todo.getTitle() + "'.");
        System.out.printf("todo: %s has an estimation of %s because: %s%n", todo.getTitle(), estimate.hours, estimate.reason);
        todo.setEstimate("" + estimate.hours);
        Todo savedTodo = repository.save(todo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Location", savedTodo.getUrl())
                .body(savedTodo);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Todo> update(@PathVariable String id, @Valid @RequestBody Todo todo) {
        Todo existingTodo = repository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        if(todo.getCompleted() != null) {
            existingTodo.setCompleted(todo.getCompleted());
        }
        if(todo.getOrder() != null) {
            existingTodo.setOrder(todo.getOrder());
        }
        if(todo.getTitle() != null) {
            existingTodo.setTitle(todo.getTitle());
        }
        Todo updatedTodo = repository.save(existingTodo);
        return ResponseEntity.ok(updatedTodo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id) {
        Todo todo = repository.findById(id).orElseThrow(() -> new TodoNotFoundException(id));
        repository.delete(todo);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAll() {
        repository.deleteAll();
        return ResponseEntity.ok().build();
    }
}
