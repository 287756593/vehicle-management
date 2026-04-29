package com.company.vehicle.controller;

import com.company.vehicle.dto.LoginRequest;
import com.company.vehicle.dto.LoginResponse;
import com.company.vehicle.entity.Driver;
import com.company.vehicle.entity.SysDept;
import com.company.vehicle.entity.SysUser;
import com.company.vehicle.security.JwtUtil;
import com.company.vehicle.service.DriverService;
import com.company.vehicle.service.SysDeptService;
import com.company.vehicle.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DriverService driverService;

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            logger.info("Login attempt for username: {}", request.getUsername());

            SysUser user = sysUserService.getByUsername(request.getUsername());
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误"));
            }

            if (user.getStatus() == null || user.getStatus() != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "账号已禁用"));
            }

            boolean passwordRequired = "DRIVER".equals(user.getRole());
            if (passwordRequired && !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                logger.error("Bad credentials for username: {}", request.getUsername());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误"));
            }

            Driver driver = null;
            if ("DRIVER".equals(user.getRole())) {
                driver = driverService.getByUserId(user.getId());
                if (driver == null || !"ACTIVE".equals(driver.getStatus())) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "驾驶员账号不可用"));
                }
            }

            logger.info("Authentication successful for username: {}", request.getUsername());

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
            return ResponseEntity.ok(buildLoginResponse(user, driver, token));
        } catch (BadCredentialsException e) {
            logger.error("Bad credentials for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "用户名或密码错误"));
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for username: {}, error: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Login error: {}", summarizeException(e));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/driver-login")
    public ResponseEntity<?> driverLogin(@RequestBody Map<String, Object> request) {
        try {
            Long driverId = Long.valueOf(request.get("driverId").toString());

            Driver driver = driverService.ensureUserAccount(driverId);
            if (driver == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "驾驶员不存在"));
            }

            if (!"ACTIVE".equals(driver.getStatus()) || driver.getUserId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "驾驶员账号不可用"));
            }

            SysUser user = sysUserService.getById(driver.getUserId());
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "账号已禁用"));
            }
            if (!"DRIVER".equals(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "当前账号不是驾驶员账号"));
            }

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("userId", user.getId());
            data.put("driverId", driver.getId());
            data.put("driverName", driver.getDriverName());
            data.put("username", user.getUsername());
            data.put("deptName", resolveDeptName(driver.getDeptId() != null ? driver.getDeptId() : user.getDeptId()));

            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Driver login error: {}", summarizeException(e));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "登录失败"));
        }
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getUserInfo(Authentication authentication) {
        String username = authentication.getName();
        SysUser user = sysUserService.getByUsername(username);
        Driver driver = "DRIVER".equals(user.getRole()) ? driverService.getByUserId(user.getId()) : null;

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        data.put("role", user.getRole());
        data.put("phone", user.getPhone());
        data.put("driverId", driver != null ? driver.getId() : null);
        data.put("deptName", resolveDeptName(driver != null && driver.getDeptId() != null ? driver.getDeptId() : user.getDeptId()));

        return ResponseEntity.ok(data);
    }

    private LoginResponse buildLoginResponse(SysUser user, Driver driver, String token) {
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setDriverId(driver != null ? driver.getId() : null);
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(user.getRole());
        response.setDeptName(resolveDeptName(driver != null && driver.getDeptId() != null ? driver.getDeptId() : user.getDeptId()));
        return response;
    }

    private String resolveDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }
        SysDept dept = sysDeptService.getById(deptId);
        return dept != null ? dept.getDeptName() : null;
    }

    private String summarizeException(Exception exception) {
        if (exception == null) {
            return "unknown";
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return exception.getClass().getSimpleName() + ": " + message;
    }
}
