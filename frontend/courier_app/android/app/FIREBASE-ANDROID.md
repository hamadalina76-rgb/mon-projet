# Firebase (Android) – notifications push

Pour que les **notifications push** fonctionnent sur l’app courier :

1. Ouvre la [Console Firebase](https://console.firebase.google.com/) et sélectionne ton projet (ex. **speedline-b5dba**).
2. **Paramètres du projet** (icône engrenage) → **Vos applications**.
3. Si l’app Android n’existe pas : **Ajouter une application** → **Android**.
   - **Nom du package Android** : `com.speedline.courier_app`
   - Enregistrer.
4. Télécharge **google-services.json** et place-le ici :
   ```
   android/app/google-services.json
   ```
5. Recompile l’app (`flutter run` ou rebuild dans l’IDE).

Sans ce fichier, l’app démarre normalement mais les push (approbation, blocage, etc.) ne seront pas reçues sur le téléphone.
