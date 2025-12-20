package ru.ambryo.gameplannerback.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ambryo.gameplannerback.dto.PlayerDto;
import ru.ambryo.gameplannerback.dto.TimeSlotDto;
import ru.ambryo.gameplannerback.entity.TimeSlot;
import ru.ambryo.gameplannerback.entity.User;
import ru.ambryo.gameplannerback.repository.TimeSlotRepository;
import ru.ambryo.gameplannerback.repository.UserRepository;

import java.time.Instant;
import java.util.Comparator;
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
    public PlayerDto updateUserProfile(User user, String name, String color, String timezone) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        managedUser.setName(name);
        managedUser.setColor(color);
        if (timezone != null && !timezone.isEmpty()) {
            managedUser.setTimezone(timezone);
        }
        User savedUser = userRepository.save(managedUser);
        return convertToDto(savedUser);
    }
    

    
    @Transactional(readOnly = true)
    public List<PlayerDto> getAllUsers() {
        return userRepository.findAll().stream()
                .filter(user -> !user.getUsername().equals("system")) // Исключаем системного пользователя
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<PlayerDto> getAllUsersWithTimeSlots(Instant startDate, Instant endDate) {
        return userRepository.findAll().stream()
                .filter(user -> !user.getUsername().equals("system")) // Исключаем системного пользователя
                .map(user -> convertToDtoWithFilter(user, startDate, endDate))
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PlayerDto getUserAsPlayerWithTimeSlots(User user, Instant startDate, Instant endDate) {
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDtoWithFilter(managedUser, startDate, endDate);
    }
    
    @Transactional
    public PlayerDto toggleTimeSlot(User user, Instant start, Integer duration) {
        // Перезагружаем пользователя в активной транзакции
        User managedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Ищем существующий слот (сравниваем по времени с точностью до часа)
        TimeSlot existingSlot = managedUser.getAvailableTimes().stream()
                .filter(ts -> {
                    Instant slotStart = ts.getStart();
                    // Сравниваем с точностью до часа (3600 секунд)
                    long startEpochHour = start.getEpochSecond() / 3600;
                    long slotStartEpochHour = slotStart.getEpochSecond() / 3600;
                    return startEpochHour == slotStartEpochHour;
                })
                .findFirst()
                .orElse(null);
        
        if (existingSlot != null) {
            // Удаляем существующий слот
            // Сохраняем ID перед удалением из коллекции
            Long slotIdToDelete = existingSlot.getId();
            // Удаляем из коллекции
            managedUser.getAvailableTimes().remove(existingSlot);
            // Удаляем из БД напрямую
            timeSlotRepository.deleteById(slotIdToDelete);
        } else {
            // Создаем новый слот
            TimeSlot newSlot = new TimeSlot(start, duration, managedUser);
            // Сохраняем в БД
            timeSlotRepository.save(newSlot);
            // Добавляем в коллекцию
            managedUser.getAvailableTimes().add(newSlot);
        }
        // Возвращаем DTO сразу, чтобы избежать повторной загрузки
        return convertToDto(managedUser);
    }
    
    private PlayerDto convertToDto(User user) {
        PlayerDto dto = new PlayerDto(user.getId(), user.getName(), user.getColor(), user.getTimezone());
        
        // Сортируем и объединяем последовательные слоты
        List<TimeSlot> sortedSlots = user.getAvailableTimes().stream()
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .collect(Collectors.toList());
        
        List<TimeSlotDto> mergedSlots = mergeConsecutiveSlots(sortedSlots);
        
        dto.setAvailableTimes(mergedSlots);
        return dto;
    }
    
    private PlayerDto convertToDtoWithFilter(User user, Instant startDate, Instant endDate) {
        PlayerDto dto = new PlayerDto(user.getId(), user.getName(), user.getColor(), user.getTimezone());
        
        // Фильтруем и объединяем временные слоты
        List<TimeSlot> filteredSlots = user.getAvailableTimes().stream()
                .filter(ts -> {
                    // Если даты не указаны, возвращаем все слоты
                    if (startDate == null && endDate == null) {
                        return true;
                    }
                    
                    Instant slotStart = ts.getStart();
                    
                    // Проверяем, попадает ли слот в диапазон
                    if (startDate != null && slotStart.isBefore(startDate)) {
                        return false;
                    }
                    return endDate == null || !slotStart.isAfter(endDate);
                })
                .sorted(Comparator.comparing(TimeSlot::getStart))
                .collect(Collectors.toList());
        
        // Объединяем последовательные слоты
        List<TimeSlotDto> mergedSlots = mergeConsecutiveSlots(filteredSlots);
        
        dto.setAvailableTimes(mergedSlots);
        return dto;
    }
    
    /**
     * Объединяет последовательные временные слоты в один с увеличенным duration
     */
    private List<TimeSlotDto> mergeConsecutiveSlots(List<TimeSlot> slots) {
        if (slots.isEmpty()) {
            return List.of();
        }
        
        List<TimeSlotDto> merged = new java.util.ArrayList<>();
        TimeSlot current = slots.getFirst();
        Instant currentEnd = current.getStart().plusSeconds(current.getDuration() * 3600L);
        int totalDuration = current.getDuration();
        
        for (int i = 1; i < slots.size(); i++) {
            TimeSlot next = slots.get(i);
            
            // Проверяем, является ли следующий слот последовательным
            // (начинается сразу после окончания текущего, с точностью до часа)
            long hoursDiff = (next.getStart().getEpochSecond() - currentEnd.getEpochSecond()) / 3600;
            
            if (hoursDiff == 0) {
                // Слоты последовательные, увеличиваем duration
                totalDuration += next.getDuration();
                currentEnd = next.getStart().plusSeconds(next.getDuration() * 3600L);
            } else {
                // Слоты не последовательные, сохраняем текущий и начинаем новый
                merged.add(new TimeSlotDto(current.getId(), current.getStart(), totalDuration));
                current = next;
                currentEnd = next.getStart().plusSeconds(next.getDuration() * 3600L);
                totalDuration = next.getDuration();
            }
        }
        
        // Добавляем последний слот
        merged.add(new TimeSlotDto(current.getId(), current.getStart(), totalDuration));
        
        return merged;
    }
}

