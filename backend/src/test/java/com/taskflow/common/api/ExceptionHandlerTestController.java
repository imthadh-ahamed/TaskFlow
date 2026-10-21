package com.taskflow.common.api;

import com.taskflow.common.ForbiddenException;
import com.taskflow.common.ResourceNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

/** Minimal controller used only by {@link GlobalExceptionHandlerTest} to trigger each exception type. */
@RestController
@RequestMapping("/test")
class ExceptionHandlerTestController {

    record Body(@NotBlank String title) {}

    @PostMapping("/validation")
    void validation(@Valid @RequestBody Body body) {}

    @GetMapping("/not-found")
    void notFound() { throw new ResourceNotFoundException("Resource 42 not found"); }

    @GetMapping("/forbidden")
    void forbidden() { throw new ForbiddenException(); }

    @GetMapping("/illegal-arg")
    void illegalArg() { throw new IllegalArgumentException("bad argument value"); }

    @GetMapping("/illegal-state")
    void illegalState() { throw new IllegalStateException("Cannot transition from TODO to DONE"); }

    @GetMapping("/data-integrity")
    void dataIntegrity() { throw new DataIntegrityViolationException("constraint violation"); }

    @GetMapping("/general")
    void general() { throw new RuntimeException("something unexpected"); }
}
