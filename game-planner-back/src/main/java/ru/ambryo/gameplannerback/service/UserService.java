package ru.ambryo.gameplannerback.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.PlayerDto;
import ru.ambryo.gameplannerback.dto.TimeSlotDto;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.entity.TimeSlot;
import ru.ambryo.gameplannerback.repository.UserRepository;
import ru.ambryo.gameplannerback.repository.TimeSlotRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    
    @Transactional(readOnly = true)
    public PlayerDto getUserAsPlayer(User user) {
        // Перезагружаем пользователя в активной транзакции для загрузки availableTimes
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDto(managedUser);
    }
    
    @Transactional(readOnly = true)
    public PlayerDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return convertToDto(user);
    }
    
    @Transactional
    public PlayerDto updateUserProfile(User user, String name, String color) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        managedUser.setName(name);
        managedUser.setColor(color);
        User savedUser = userRepository.save(managedUser);
        return convertToDto(savedUser);
    }
    

    
    @Transactional(readOnly = true)
    public List<PlayerDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void toggleTimeSlot(User user, LocalDateTime start, Integer duration) {
        // Перезагружаем пользователя в активной транзакции
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Ищем существующий слот
        TimeSlot existingSlot = managedUser.getAvailableTimes().stream()
                .filter(ts -> {
                    LocalDateTime slotStart = ts.getStart();
                    return slotStart.toLocalDate().equals(start.toLocalDate()) &&
                           slotStart.getHour() == start.getHour();
                })
                .findFirst()
                .orElse(null);
        
        if (existingSlot != null) {
            // Удаляем существующий слот
            managedUser.getAvailableTimes().remove(existingSlot);
            timeSlotRepository.delete(existingSlot);
        } else {
            // Создаем новый слот
            TimeSlot newSlot = new TimeSlot(start, duration, managedUser);
            managedUser.getAvailableTimes().add(newSlot);
            timeSlotRepository.save(newSlot);
        }
    }
    
    private PlayerDto convertToDto(User user) {
        PlayerDto dto = new PlayerDto(user.getId(), user.getName(), user.getColor());
        
        List<TimeSlotDto> timeSlotDtos = user.getAvailableTimes().stream()
                .map(ts -> new TimeSlotDto(ts.getId(), ts.getStart(), ts.getDuration()))
                .collect(Collectors.toList());
        
        dto.setAvailableTimes(timeSlotDtos);
        return dto;
    }
}

