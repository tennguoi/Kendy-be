// package com.example.KendyDigital.config;

// import org.springframework.boot.CommandLineRunner;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.annotation.Transactional;

// import com.example.KendyDigital.model.AdminRole;
// import com.example.KendyDigital.model.UserAccount;
// import com.example.KendyDigital.model.UserAdminRole;
// import com.example.KendyDigital.model.UserRole;
// import com.example.KendyDigital.repository.AdminRoleRepository;
// import com.example.KendyDigital.repository.UserAccountRepository;
// import com.example.KendyDigital.repository.UserAdminRoleRepository;

// @Component
// public class DataInitializer implements CommandLineRunner {

//     private final UserAccountRepository userAccountRepository;
//     private final AdminRoleRepository adminRoleRepository;
//     private final UserAdminRoleRepository userAdminRoleRepository;
//     private final PasswordEncoder passwordEncoder;

//     public DataInitializer(UserAccountRepository userAccountRepository,
//                            AdminRoleRepository adminRoleRepository,
//                            UserAdminRoleRepository userAdminRoleRepository,
//                            PasswordEncoder passwordEncoder) {
//         this.userAccountRepository = userAccountRepository;
//         this.adminRoleRepository = adminRoleRepository;
//         this.userAdminRoleRepository = userAdminRoleRepository;
//         this.passwordEncoder = passwordEncoder;
//     }

//     @Override
//     @Transactional
//     public void run(String... args) {
//         AdminRole adminRole = createRoleIfNotExists("admin", "Administrator role with full system access");
//         AdminRole doctorRole = createRoleIfNotExists("bac_si", "Doctor role for medical professionals");
//         AdminRole receptionistRole = createRoleIfNotExists("tiep_tan", "Receptionist role for front desk staff");

//         if (!userAccountRepository.existsByEmailIgnoreCase("admin@clinic.com")) {
//             UserAccount admin = new UserAccount(
//                     "Nguyễn Văn Admin",
//                     "nguyenduccongminh34@gmail.com",
//                     "0123456789",
//                     passwordEncoder.encode("admin123"));
//             admin.setRole(UserRole.SUPER_ADMIN);
//             admin = userAccountRepository.save(admin);

//             UserAdminRole adminUserRole = new UserAdminRole(admin, adminRole);
//             userAdminRoleRepository.save(adminUserRole);
//             System.out.println("Created admin user: nguyenduccongminh34@gmail.com / admin123");
//         }

//         if (!userAccountRepository.existsByEmailIgnoreCase("user@gmail.com")) {
//             UserAccount user = new UserAccount(
//                     "Nguyễn Văn User",
//                     "user@gmail.com",
//                     "0901234567",
//                     passwordEncoder.encode("user123"));
//             user.setRole(UserRole.USER);
//             userAccountRepository.save(user);
//             System.out.println("Created standard user: user@gmail.com / user123");
//         }

//         // if (!userAccountRepository.existsByEmailIgnoreCase("congminh152005@gmail.com")) {
//         //     UserAccount doctor = new UserAccount(
//         //             "Phạm Minh Đàn",
//         //             "congminh152005@gmail.com",
//         //             "0987654321",
//         //             passwordEncoder.encode("doctor123"));
//         //     doctor.setRole(UserRole.ADMIN);
//         //     doctor = userAccountRepository.save(doctor);

//         //     UserAdminRole doctorUserRole = new UserAdminRole(doctor, doctorRole);
//         //     userAdminRoleRepository.save(doctorUserRole);
//         //     System.out.println("Created doctor user: congminh152005@gmail.com / doctor123");
//         // }

//         // if (!userAccountRepository.existsByEmailIgnoreCase("danpham261005@gmail.com")) {
//         //     UserAccount receptionist = new UserAccount(
//         //             "Nguyễn Danh Hiếu",
//         //             "danpham261005@gmail.com",
//         //             "0912345678",
//         //             passwordEncoder.encode("receptionist123"));
//         //     receptionist.setRole(UserRole.ADMIN);
//         //     receptionist = userAccountRepository.save(receptionist);

//         //     UserAdminRole receptionistUserRole = new UserAdminRole(receptionist, receptionistRole);
//         //     userAdminRoleRepository.save(receptionistUserRole);
//         //     System.out.println("Created receptionist user: danpham261005@gmail.com / receptionist123");
//         // }

//         System.out.println("Data initialization completed!");
//     }

//     private AdminRole createRoleIfNotExists(String name, String description) {
//         return adminRoleRepository.findByName(name)
//                 .orElseGet(() -> {
//                     AdminRole role = new AdminRole(name, description, true);
//                     return adminRoleRepository.save(role);
//                 });
//     }
// }
