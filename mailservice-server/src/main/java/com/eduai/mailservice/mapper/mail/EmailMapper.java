package com.eduai.mailservice.mapper.mail;

import com.eduai.mailservice.dto.request.EmailRequestDto;
import com.eduai.mailservice.dto.response.EmailResponseDto;
import com.eduai.mailservice.entity.mail.EmailLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for Email domain objects.
 */
@Mapper(componentModel = "spring")
public interface EmailMapper {

    @Mapping(target = "messageId", source = "id")
    @Mapping(target = "accepted",  constant = "true")
    @Mapping(target = "queued",    constant = "false")
    @Mapping(target = "acceptedAt", source = "createdAt")
    @Mapping(target = "sentAt",   source = "sentAt")
    @Mapping(target = "errors",   ignore = true)
    @Mapping(target = "estimatedDelivery", ignore = true)
    EmailResponseDto toResponseDto(EmailLog emailLog);
}
