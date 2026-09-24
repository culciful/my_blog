package com.culciful.service;

import com.culciful.common.api.R;
import com.culciful.dto.EmailExistParam;
import com.culciful.dto.RegisterRequest;
import java.util.Map;

public interface UserService {

    R<Void> register(RegisterRequest request);

    R<Map<String, Boolean>> checkEmailExist(EmailExistParam emailExistParam);
}
