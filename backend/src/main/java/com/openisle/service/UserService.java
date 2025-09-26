package com.openisle.service;

import com.openisle.config.CachingConfig;
import com.openisle.exception.FieldException;
import com.openisle.model.Role;
import com.openisle.model.User;
import com.openisle.repository.UserRepository;
import com.openisle.search.SearchIndexEventPublisher;
import com.openisle.service.AvatarGenerator;
import com.openisle.service.PasswordValidator;
import com.openisle.service.UsernameValidator;
import com.openisle.util.VerifyType;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordValidator passwordValidator;
  private final UsernameValidator usernameValidator;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final ImageUploader imageUploader;
  private final AvatarGenerator avatarGenerator;

  private final RedisTemplate redisTemplate;

  private final EmailSender emailService;
  private final SearchIndexEventPublisher searchIndexEventPublisher;

  public User register(
    String username,
    String email,
    String password,
    String reason,
    com.openisle.model.RegisterMode mode
  ) {
    usernameValidator.validate(username);
    passwordValidator.validate(password);
    // ── 先按用户名查 ──────────────────────────────────────────
    Optional<User> byUsername = userRepository.findByUsername(username);
    if (byUsername.isPresent()) {
      User u = byUsername.get();
      if (u.isVerified()) {
        // 已验证 → 直接拒绝
        throw new FieldException("username", "User name already exists");
      }
      // 未验证 → 允许“重注册”：覆盖必要字段并重新发验证码
      u.setEmail(email); // 若不允许改邮箱可去掉
      u.setPassword(passwordEncoder.encode(password));
      //            u.setVerificationCode(genCode());
      u.setRegisterReason(reason);
      u.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
      User saved = userRepository.save(u);
      searchIndexEventPublisher.publishUserSaved(saved);
      return saved;
    }

    // ── 再按邮箱查 ───────────────────────────────────────────
    Optional<User> byEmail = userRepository.findByEmail(email);
    if (byEmail.isPresent()) {
      User u = byEmail.get();
      if (u.isVerified()) {
        // 已验证 → 直接拒绝
        throw new FieldException("email", "User email already exists");
      }
      // 未验证 → 允许“重注册”
      u.setUsername(username); // 若不允许改用户名可去掉
      u.setPassword(passwordEncoder.encode(password));
      //            u.setVerificationCode(genCode());
      u.setRegisterReason(reason);
      u.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
      User saved = userRepository.save(u);
      searchIndexEventPublisher.publishUserSaved(saved);
      return saved;
    }

    // ── 完全新用户 ───────────────────────────────────────────
    User user = new User();
    user.setUsername(username);
    user.setEmail(email);
    user.setPassword(passwordEncoder.encode(password));
    user.setRole(Role.USER);
    user.setVerified(false);
    //        user.setVerificationCode(genCode());
    user.setAvatar(avatarGenerator.generate(username));
    user.setRegisterReason(reason);
    user.setApproved(mode == com.openisle.model.RegisterMode.DIRECT);
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return saved;
  }

  public User registerWithInvite(String username, String email, String password) {
    User user = register(username, email, password, "", com.openisle.model.RegisterMode.DIRECT);
    user.setVerified(true);
    //        user.setVerificationCode(genCode());
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return saved;
  }

  private String genCode() {
    return String.format("%06d", new Random().nextInt(1000000));
  }

  /**
   * 将验证码存入缓存，并发送邮件
   * @param user
   */
  public void sendVerifyMail(User user, VerifyType verifyType) {
    // 缓存验证码
    String code = genCode();
    String key;
    String subject;
    String content = "您的验证码是:" + code;
    // 注册类型
    if (verifyType.equals(VerifyType.REGISTER)) {
      key = CachingConfig.VERIFY_CACHE_NAME + ":register:code:" + user.getUsername();
      subject = "在网站填写验证码以验证(有效期为5分钟)";
    } else {
      // 重置密码
      key = CachingConfig.VERIFY_CACHE_NAME + ":reset_password:code:" + user.getUsername();
      subject = "请填写验证码以重置密码(有效期为5分钟)";
    }

    redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES); // 五分钟后验证码过期
    emailService.sendEmail(user.getEmail(), subject, content);
  }

  /**
   * 验证code是否正确
   * @param user
   * @param code
   * @param verifyType
   * @return
   */
  public boolean verifyCode(User user, String code, VerifyType verifyType) {
    // 生成key
    String key1 = VerifyType.REGISTER.equals(verifyType)
      ? ":register:code:"
      : ":reset_password:code:";
    String key = CachingConfig.VERIFY_CACHE_NAME + key1 + user.getUsername();
    // 这里不能使用getAndDelete,需要6.x版本
    String cachedCode = (String) redisTemplate.opsForValue().get(key);
    // 如果校验code过期或者不存在
    // 或者校验code不一致
    if (Objects.isNull(cachedCode) || !cachedCode.equals(code)) {
      return false;
    }
    // 注册模式需要设置已经确认
    if (VerifyType.REGISTER.equals(verifyType)) {
      user.setVerified(true);
      userRepository.save(user);
    }
    // 走到这里说明验证成功删除验证码
    redisTemplate.delete(key);
    return true;
  }

  public Optional<User> authenticate(String username, String password) {
    return userRepository
      .findByUsername(username)
      .filter(User::isVerified)
      .filter(User::isApproved)
      .filter(user -> passwordEncoder.matches(password, user.getPassword()));
  }

  public boolean matchesPassword(User user, String rawPassword) {
    return passwordEncoder.matches(rawPassword, user.getPassword());
  }

  public Optional<User> findByUsername(String username) {
    return userRepository.findByUsername(username);
  }

  public Optional<User> findByEmail(String email) {
    return userRepository.findByEmail(email);
  }

  public Optional<User> findById(Long id) {
    return userRepository.findById(id);
  }

  public Optional<User> findByIdentifier(String identifier) {
    if (identifier.matches("\\d+")) {
      return userRepository.findById(Long.parseLong(identifier));
    }
    return userRepository.findByUsername(identifier);
  }

  public User updateAvatar(String username, String avatarUrl) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    String old = user.getAvatar();
    user.setAvatar(avatarUrl);
    User saved = userRepository.save(user);
    if (old != null && !old.equals(avatarUrl)) {
      imageUploader.removeReferences(java.util.Set.of(old));
    }
    if (avatarUrl != null) {
      imageUploader.addReferences(java.util.Set.of(avatarUrl));
    }
    return saved;
  }

  public User updateReason(String username, String reason) {
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    user.setRegisterReason(reason);
    User saved = userRepository.save(user);
    searchIndexEventPublisher.publishUserSaved(saved);
    return saved;
  }

  public User updateProfile(String currentUsername, String newUsername, String introduction) {
    User user = userRepository
      .findByUsername(currentUsername)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    if (newUsername != null && !newUsername.equals(currentUsername)) {
      usernameValidator.validate(newUsername);
      userRepository
        .findByUsername(newUsername)
        .ifPresent(u -> {
          throw new FieldException("username", "User name already exists");
        });
      user.setUsername(newUsername);
    }
    if (introduction != null) {
      user.setIntroduction(introduction);
    }
    return userRepository.save(user);
  }

  public User updatePassword(String username, String newPassword) {
    passwordValidator.validate(newPassword);
    User user = userRepository
      .findByUsername(username)
      .orElseThrow(() -> new com.openisle.exception.NotFoundException("User not found"));
    user.setPassword(passwordEncoder.encode(newPassword));
    return userRepository.save(user);
  }

  /**
   * Get all administrator accounts.
   */
  public java.util.List<User> getAdmins() {
    return userRepository.findByRole(Role.ADMIN);
  }
}
