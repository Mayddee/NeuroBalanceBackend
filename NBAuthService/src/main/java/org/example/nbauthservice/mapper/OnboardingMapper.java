package org.example.nbauthservice.mapper;

import org.example.nbauthservice.dto.OnboardingDTO;
import org.example.nbauthservice.entity.UserOnboarding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    @Mapping(source = "user.id", target = "userId")
    OnboardingDTO toDto(UserOnboarding onboarding);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    UserOnboarding toEntity(OnboardingDTO dto);
}