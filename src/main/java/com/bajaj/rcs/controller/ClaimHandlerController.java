package com.bajaj.rcs.controller;

import com.bajaj.rcs.dto.RcsDto.HandlerRequest;
import com.bajaj.rcs.dto.RcsDto.LeaveRequest;
import com.bajaj.rcs.dto.RcsDto.MappingRequest;
import com.bajaj.rcs.entity.ClaimHandler;
import com.bajaj.rcs.entity.HandlerLeave;
import com.bajaj.rcs.service.ClaimHandlerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/handlers")
public class ClaimHandlerController {

    private final ClaimHandlerService handlerService;

    public ClaimHandlerController(ClaimHandlerService handlerService) {
        this.handlerService = handlerService;
    }

    // Create a handler
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimHandler create(@Valid @RequestBody HandlerRequest request) {
        return handlerService.create(request);
    }

    // Update a handler
    @PutMapping("/{handlerCode}")
    public ClaimHandler update(@PathVariable String handlerCode,
                               @Valid @RequestBody HandlerRequest request) {
        return handlerService.update(handlerCode, request);
    }

    // Fetch all handlers
    @GetMapping
    public List<ClaimHandler> getHandlers() {
        return handlerService.findAll();
    }

    // Fetch one handler
    @GetMapping("/{handlerCode}")
    public ClaimHandler getHandler(@PathVariable String handlerCode) {
        return handlerService.findByCode(handlerCode);
    }

    //Adding more locations / policy numbers / insured names to a handler
    @PostMapping("/{handlerCode}/mappings")
    public ClaimHandler addMappings(@PathVariable String handlerCode,
                                    @Valid @RequestBody MappingRequest request) {
        return handlerService.addMappings(handlerCode, request);
    }

    // Mark the handler on leave or half day
    @PostMapping("/{handlerCode}/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public HandlerLeave addLeave(@PathVariable String handlerCode,
                                 @Valid @RequestBody LeaveRequest request) {
        return handlerService.addLeave(handlerCode, request);
    }
}
