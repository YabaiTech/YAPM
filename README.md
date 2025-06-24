# YAPM (Yet Another Password Manager)

YAPM (Yet Another Password Manager) is a simple, secure, and flexible password manager that works online. Designed with usability and privacy in mind, YAPM helps you securely store and manage your passwords and sensitive credentials without compromising on convenience or security.

## Features

- **Secure Storage:** All passwords and sensitive data are encrypted using strong cryptographic algorithms.
- **User Authentication:** Secure login system to protect access to your password vault.
- **Intuitive UI:** Simple and clean user interface for easy management of credentials.
- **Password Generation:** Built-in strong password generator to help you create secure passwords as well as strength detection.
- **Cross-Platform:** Designed for use on multiple operating systems.
- **Extensible:** Modular architecture for adding new features or integrations.

## Implementation Details

### Technology Stack

- **Language:** Java
- **Encryption:** Uses industry-standard cryptographic libraries (e.g., AES) to encrypt all sensitive data before storage.
- **Persistence:** Supports remote database/server storage (for online mode).
- **User Interface:** Built with Java Swing ([FlatLaf library](https://github.com/JFormDesigner/FlatLaf)) for a responsive desktop experience.
- **Dependency Management:** Uses Gradle for managing dependencies and builds.

### Core Modules

1. **Authentication Module**

   - Manages user registration, login, and session management.
   - Passwords are salted and hashed using secure algorithms (e.g., PBKDF2, bcrypt).
   - MYSQL database is utilized for storing user credentials as well as a timestamp for last login.

2. **Encryption Module**

   - Handles all encryption/decryption operations.
   - Utilizes a master password to derive encryption keys.
   - Ensures that decrypted data is only available in memory when needed.

3. **Storage Module**

   - Treats storage as remote vaults.

4. **Password Management Module**

   - CRUD operations (Create, Read, Update, Delete) for password entries.

5. **UI Module**

   - Presents an intuitive interface for managing credentials.
   - Includes addition, deletion, and edition capabilities.

6. **Password Generator**
   - Generates strong, random passwords including lowercase alphabets, uppercase alphabets, digits, and a collection of 32 special characters.
   - Displays the strength of generated/manually typed password.

### Security Considerations

- **Zero-Knowledge:** Master password is never stored or transmitted; only the user knows it.
- **Secure Memory Handling:** Sensitive data is cleared from memory as soon as it is no longer needed.
- **Regular Security Audits:** Code is structured for easy review and audit.

### Encryption Mechanisms

| Purpose          | Algorithm            | Parameters                                  |
| ---------------- | -------------------- | ------------------------------------------- |
| Data encryption  | AES/CBC/PKCS5Padding | 256-bit key, 16B IV, 16B salt, PKCS5Padding |
| Key derivation   | PBKDF2WithHmacSHA256 | 65,536 iterations, 256-bit key, 16B salt    |
| Password hashing | PBKDF2WithHmacSHA1   | 65,536 iterations, 128-bit key, 16B salt    |

### Example Usage

1. **First-Time Setup**

   - User creates an account which associates with a master password, with which an encrypted vault is initialized.

2. **Adding a Password**

   - User adds a new entry; data is encrypted and stored remotely.

## Getting Started

For unix-like operating systems.

1. **Clone the Repository**

   ```sh
   git clone https://github.com/YabaiTech/YAPM.git
   cd YAPM
   ```

2. **Get Crendentials for Cloud DB and Cloud Storage and Copy it Over to app/.env**

   ```sh
   cp prod.env app/.env
   ```

3. **Build the Project**

   ```sh
   ./gradlew build
   ```

4. **Run the Application**
   ```sh
   ./gradlew run
   ```

## Limitations

- Not an offline password manager, which is theoretically more secure.
- Due to using the free tier of Supabase as cloud storage of the vaults, the sycning is not seemless. The caching delay holds back the updation of the vault across multiple devices.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Contributing

Don't bother.
