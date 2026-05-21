package com.autoadda.apis.identity.service;

import com.autoadda.apis.identity.response.AdminUserResponse;
import com.autoadda.apis.identity.response.AdminUserStatsResponse;
import com.autoadda.apis.identity.response.GenericResponse;
import com.autoadda.apis.identity.util.ERole;
import com.autoadda.apis.identity.util.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserResponse> listUsers(String search, ERole role, Status status, Pageable pageable);

    AdminUserStatsResponse getStats();

    GenericResponse updateStatus(Integer id, Status status);

    GenericResponse softDelete(Integer id);
}
