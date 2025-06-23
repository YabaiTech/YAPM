# YAPM (Yet Another Password Manager)

YAPM (Yet Another Password Manager) is a simple, secure, and flexible password manager that works both online and offline. Designed with usability and privacy in mind, YAPM helps you securely store and manage your passwords and sensitive credentials without compromising on convenience or security.

## Features

- **Online/Offline Support:** Use YAPM seamlessly whether you're connected to the internet or not. Your data is always accessible.
- **Secure Storage:** All passwords and sensitive data are encrypted using strong cryptographic algorithms.
- **User Authentication:** Secure login system to protect access to your password vault.
- **Intuitive UI:** Simple and clean user interface for easy management of credentials.
- **Password Generation:** Built-in strong password generator to help you create secure passwords.
- **Cross-Platform:** Designed for use on multiple operating systems.
- **Extensible:** Modular architecture for adding new features or integrations.

## Implementation Details

### Technology Stack

- **Language:** Java
- **Encryption:** Uses industry-standard cryptographic libraries (e.g., AES) to encrypt all sensitive data before storage.
- **Persistence:** Supports both local file-based storage (for offline mode) and remote database/server storage (for online mode).
- **User Interface:** Built with Java Swing ([FlatLaf library](https://github.com/JFormDesigner/FlatLaf)) for a responsive desktop experience.
- **Dependency Management:** Uses Gradle for managing dependencies and builds.

### Core Modules

1. **Authentication Module**

   - Manages user registration, login, and session management.
   - Passwords are salted and hashed using secure algorithms (e.g., PBKDF2, bcrypt).

2. **Encryption Module**

   - Handles all encryption/decryption operations.
   - Utilizes a master password to derive encryption keys.
   - Ensures that decrypted data is only available in memory when needed.

3. **Storage Module**

   - Abstracts storage to support both local and remote options.
   - Enables seamless switching between online and offline operation.

4. **Password Management Module**

   - CRUD operations (Create, Read, Update, Delete) for password entries.
   - Supports additional fields such as notes, URLs, and categories.

5. **UI Module**

   - Presents an intuitive interface for managing credentials.
   - Includes addition, deletion, and edition capabilities.

6. **Password Generator**
   - Generates strong, random passwords including lowercase alphabets, uppercase alphabets, digits, and a collection of 32 special characters.

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

   - User adds a new entry; data is encrypted and stored both locally and remotely.

3. **Switching Modes**
   - User can turn off the internet and enter offline mode while the password manager still functions.

## Getting Started

1. **Clone the Repository**

   ```sh
   git clone https://github.com/YabaiTech/YAPM.git
   cd YAPM
   ```

2. **Build the Project**

   ```sh
   ./gradlew build
   ```

3. **Run the Application**
   ```sh
   ./gradlew run
   ```

## Contributing

Don't bother.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Disclaimer

YAPM is provided as-is, without warranty of any kind. Use at your own risk.
