package com.microfinance.service;

import com.microfinance.dto.UserProfileDto;
import com.microfinance.entity.User;
import com.microfinance.entity.UserProfile;
import com.microfinance.repository.UserProfileRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        log.info("Fetching profile for user: {}", username);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        return userProfileRepository.findByUserId(user.getId())
                .map(this::mapToDto)
                .orElse(new UserProfileDto()); // Return empty DTO if no profile exists
    }

    @Transactional
    public UserProfileDto updateProfile(String username, UserProfileDto dto) {
        log.info("Updating profile for user: {}", username);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        // Update fields
        profile.setFirstName(dto.getFirstName());
        profile.setLastName(dto.getLastName());
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setGender(dto.getGender());
        profile.setPhoneNumber(dto.getPhoneNumber());
        profile.setAddressLine1(dto.getAddressLine1());
        profile.setAddressLine2(dto.getAddressLine2());
        profile.setCity(dto.getCity());
        profile.setState(dto.getState());
        profile.setPincode(dto.getPincode());
        profile.setPanNumber(dto.getPanNumber());
        profile.setAadhaarNumber(dto.getAadhaarNumber());
        profile.setEmploymentType(dto.getEmploymentType());
        profile.setMonthlyIncome(dto.getMonthlyIncome());

        UserProfile savedProfile = userProfileRepository.save(profile);
        return mapToDto(savedProfile);
    }

    @Transactional
    public UserProfileDto submitKyc(String username) {
        log.info("Submitting Profile KYC for verification for user: {}", username);
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        UserProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> UserProfile.builder().user(user).build());

        if (!profile.isKycVerified()) {
            profile.setKycStatus("PENDING");
            userProfileRepository.save(profile);
        }
        return mapToDto(profile);
    }

    private UserProfileDto mapToDto(UserProfile profile) {
        return UserProfileDto.builder()
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .phoneNumber(profile.getPhoneNumber())
                .addressLine1(profile.getAddressLine1())
                .addressLine2(profile.getAddressLine2())
                .city(profile.getCity())
                .state(profile.getState())
                .pincode(profile.getPincode())
                .panNumber(profile.getPanNumber())
                .aadhaarNumber(profile.getAadhaarNumber())
                .employmentType(profile.getEmploymentType())
                .monthlyIncome(profile.getMonthlyIncome())
                .kycVerified(profile.isKycVerified())
                .kycStatus(profile.getKycStatus())
                .build();
    }
}
