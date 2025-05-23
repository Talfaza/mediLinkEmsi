# MediLink - Système de Gestion de Santé

[English version below](#english-version)

MediLink est un système complet de gestion de santé composé de trois composants principaux :
- Application Web Frontend
- Application Mobile
- Serveur Backend

## Prérequis

- Node.js (v14 ou supérieur)
- Java JDK 11 ou supérieur
- Maven
- Android Studio (pour le développement mobile)
- Docker et Docker Compose (pour le déploiement conteneurisé)

## Structure du Projet

```
medilink/
├── medi_front/     # Application web frontend
├── medi_mobile/    # Application mobile Android
└── target/         # Serveur backend
```

## Instructions d'Installation et d'Exécution

### Serveur Backend

1. Accédez au répertoire backend :
   ```bash
   cd target
   ```

2. Construisez le projet avec Maven :
   ```bash
   mvn clean install
   ```

3. Exécutez l'application :
   ```bash
   mvn spring-boot:run
   ```
   Le serveur démarrera sur le port 8080 par défaut.

### Application Web Frontend

1. Accédez au répertoire frontend :
   ```bash
   cd medi_front
   ```

2. Installez les dépendances :
   ```bash
   npm install
   ```

3. Démarrez le serveur de développement :
   ```bash
   npm start
   ```
   Le frontend sera disponible à l'adresse http://localhost:3000

### Application Mobile

1. Ouvrez Android Studio
2. Sélectionnez "Ouvrir un projet existant"
3. Naviguez vers le répertoire `medi_mobile` et ouvrez-le
4. Attendez que le projet se synchronise et télécharge les dépendances
5. Connectez un appareil Android ou démarrez un émulateur
6. Cliquez sur le bouton "Exécuter" (icône de lecture verte) pour construire et exécuter l'application

## Déploiement

### Utilisation de Docker Compose

La méthode la plus simple pour déployer l'application complète est d'utiliser Docker Compose :

1. Assurez-vous que Docker et Docker Compose sont installés sur votre système
2. Depuis le répertoire racine, exécutez :
   ```bash
   docker-compose up -d
   ```

Cela démarrera tous les services :
- Serveur backend sur le port 8080
- Application frontend sur le port 3000
- Base de données (si configurée)

### Déploiement Manuel

#### Backend
1. Construisez le backend :
   ```bash
   mvn clean package
   ```
2. Déployez le fichier WAR généré sur votre serveur Tomcat

#### Frontend
1. Construisez le frontend :
   ```bash
   cd medi_front
   npm run build
   ```
2. Déployez le contenu du répertoire `dist` sur votre serveur web

#### Application Mobile
1. Générez un APK signé via Android Studio
2. Distribuez l'APK via votre canal de distribution préféré

## Variables d'Environnement

### Backend
- `DB_URL`: URL de connexion à la base de données
- `DB_USERNAME`: Nom d'utilisateur de la base de données
- `DB_PASSWORD`: Mot de passe de la base de données
- `JWT_SECRET`: Clé secrète pour la génération de token JWT

### Frontend
- `REACT_APP_API_URL`: URL de l'API backend
- `REACT_APP_ENV`: Environnement (développement/production)

### Mobile
- `API_BASE_URL`: URL de l'API backend

## Dépannage

### Problèmes Courants

1. Conflits de Ports
   - Si le port 8080 est déjà utilisé, vous pouvez changer le port du backend dans `application.properties`
   - Si le port 3000 est utilisé, vous pouvez changer le port du frontend dans `package.json`

2. Connexion à la Base de Données
   - Assurez-vous que la base de données est en cours d'exécution et accessible
   - Vérifiez les identifiants de la base de données dans les variables d'environnement

3. Problèmes de Construction de l'Application Mobile
   - Nettoyez et reconstruisez le projet dans Android Studio
   - Assurez-vous que toutes les versions SDK sont compatibles

## Contribution

1. Forkez le dépôt
2. Créez une branche de fonctionnalité
3. Committez vos modifications
4. Poussez vers la branche
5. Créez une Pull Request

## Licence

Ce projet est sous licence MIT - voir le fichier LICENSE pour plus de détails.

---

# English Version

MediLink is a comprehensive healthcare management system consisting of three main components:
- Frontend Web Application
- Mobile Application
- Backend Server

## Prerequisites

- Node.js (v14 or higher)
- Java JDK 11 or higher
- Maven
- Android Studio (for mobile development)
- Docker and Docker Compose (for containerized deployment)

## Project Structure

```
medilink/
├── medi_front/     # Frontend web application
├── medi_mobile/    # Android mobile application
└── target/         # Backend server
```

## Setup and Running Instructions

### Backend Server

1. Navigate to the backend directory:
   ```bash
   cd target
   ```

2. Build the project using Maven:
   ```bash
   mvn clean install
   ```

3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
   The server will start on port 8080 by default.

### Frontend Web Application

1. Navigate to the frontend directory:
   ```bash
   cd medi_front
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm start
   ```
   The frontend will be available at http://localhost:3000

### Mobile Application

1. Open Android Studio
2. Select "Open an existing project"
3. Navigate to the `medi_mobile` directory and open it
4. Wait for the project to sync and download dependencies
5. Connect an Android device or start an emulator
6. Click the "Run" button (green play icon) to build and run the application

## Deployment

### Using Docker Compose

The easiest way to deploy the entire application is using Docker Compose:

1. Make sure Docker and Docker Compose are installed on your system
2. From the root directory, run:
   ```bash
   docker-compose up -d
   ```

This will start all services:
- Backend server on port 8080
- Frontend application on port 3000
- Database (if configured)

### Manual Deployment

#### Backend
1. Build the backend:
   ```bash
   mvn clean package
   ```
2. Deploy the generated WAR file to your Tomcat server

#### Frontend
1. Build the frontend:
   ```bash
   cd medi_front
   npm run build
   ```
2. Deploy the contents of the `dist` directory to your web server

#### Mobile App
1. Generate a signed APK through Android Studio
2. Distribute the APK through your preferred distribution channel

## Environment Variables

### Backend
- `DB_URL`: Database connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Secret key for JWT token generation

### Frontend
- `REACT_APP_API_URL`: Backend API URL
- `REACT_APP_ENV`: Environment (development/production)

### Mobile
- `API_BASE_URL`: Backend API URL

## Troubleshooting

### Common Issues

1. Port Conflicts
   - If port 8080 is already in use, you can change the backend port in `application.properties`
   - If port 3000 is in use, you can change the frontend port in `package.json`

2. Database Connection
   - Ensure the database is running and accessible
   - Check database credentials in environment variables

3. Mobile App Build Issues
   - Clean and rebuild the project in Android Studio
   - Ensure all SDK versions are compatible

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
