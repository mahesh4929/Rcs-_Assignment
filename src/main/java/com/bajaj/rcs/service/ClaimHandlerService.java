package com.bajaj.rcs.service;

import com.bajaj.rcs.dto.RcsDto.HandlerRequest;
import com.bajaj.rcs.dto.RcsDto.LeaveRequest;
import com.bajaj.rcs.dto.RcsDto.MappingRequest;
import com.bajaj.rcs.entity.ClaimHandler;
import com.bajaj.rcs.entity.HandlerLeave;
import com.bajaj.rcs.repository.ClaimHandlerRepository;
import com.bajaj.rcs.repository.HandlerLeaveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClaimHandlerService {

    private final ClaimHandlerRepository handlerRepository;
    private final HandlerLeaveRepository leaveRepository;

    public ClaimHandlerService(ClaimHandlerRepository handlerRepository,
                               HandlerLeaveRepository leaveRepository) {
        this.handlerRepository = handlerRepository;
        this.leaveRepository = leaveRepository;
    }

    // Fetch all handlers.
    public List<ClaimHandler> findAll() {
        return handlerRepository.findAll();
    }

    // Fetch one handler by code
    public ClaimHandler findByCode(String handlerCode) {
        return find(handlerCode);
    }

    // Git practice: feature branch modification
    @Transactional
    public ClaimHandler create(HandlerRequest r) {
        if (handlerRepository.findByHandlerCode(r.handlerCode()).isPresent()) {
            throw new IllegalStateException("Handler already exists: " + r.handlerCode());
        }
        if (r.maxAmount().compareTo(r.minAmount()) < 0) {
            throw new IllegalStateException("maxAmount must be greater than minAmount");
        }
        ClaimHandler handler = new ClaimHandler(r.handlerCode(), r.handlerName(), r.lob(),
                r.minAmount(), r.maxAmount(), r.active());

        r.locations().forEach(v -> handler.addMapping("LOCATION", v));
        if (r.policyNumbers() != null) r.policyNumbers().forEach(v -> handler.addMapping("POLICY_NUMBER", v));
        if (r.insuredNames() != null)  r.insuredNames().forEach(v -> handler.addMapping("INSURED_NAME", v));

        ClaimHandler saved = handlerRepository.save(handler);   // mappings saved too (cascade)
        saveLeave(saved, r.leaveRequest());                     // leave saved if sent
        return saved;    }

    /** Update a handler. Locations / policy numbers / insured names are replaced by the new lists. */
    @Transactional
    public ClaimHandler update(String handlerCode, HandlerRequest r) {
        ClaimHandler handler = find(handlerCode);
        if (!handler.getHandlerCode().equals(r.handlerCode())) {
            throw new IllegalStateException("Handler code cannot be changed: " + handlerCode + " -> " + r.handlerCode());
        }
        if (r.maxAmount().compareTo(r.minAmount()) < 0) {
            throw new IllegalStateException("maxAmount must be greater than minAmount");
        }
        handler.update(r.handlerName(), r.lob(), r.minAmount(), r.maxAmount(), r.active());
        handler.replaceMappings("LOCATION", r.locations());
        handler.replaceMappings("POLICY_NUMBER", r.policyNumbers());
        handler.replaceMappings("INSURED_NAME", r.insuredNames());
        saveLeave(handler, r.leaveRequest());
        return handler;   // saved automatically at the end of the transaction
    }

    /** "Add" button - add more locations / policy numbers / insured names. */
    @Transactional
    public ClaimHandler addMappings(String handlerCode, MappingRequest r) {
        ClaimHandler handler = find(handlerCode);
        r.values().forEach(v -> handler.addMapping(r.mappingType(), v));
        return handler;
    }

    @Transactional
    public HandlerLeave addLeave(String handlerCode, LeaveRequest r) {
        return saveLeave(find(handlerCode), r);
    }

    /** Saves a leave for the handler. Does nothing if no leave was sent. */
    private HandlerLeave saveLeave(ClaimHandler handler, LeaveRequest r) {
        if (r == null) return null;
        if (!r.leaveTo().isAfter(r.leaveFrom())) {
            throw new IllegalStateException("leaveTo must be after leaveFrom");
        }
        return leaveRepository.save(new HandlerLeave(handler, r.leaveFrom(), r.leaveTo(), r.halfDay()));
    }

    private ClaimHandler find(String handlerCode) {
        return handlerRepository.findByHandlerCode(handlerCode)
                .orElseThrow(() -> new IllegalArgumentException("Handler not found: " + handlerCode));
    }
}
