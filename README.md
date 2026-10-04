# Minsapp

**WhatsApp officiel, sans Meta AI, sans Statuts, sans Chaînes.** Pour Android.

Minsapp n'est pas un « faux WhatsApp ». Tu gardes l'application officielle, avec tout ce qu'elle fait : discussions, groupes, appels, vocaux, notifications, sauvegardes. Minsapp est un petit **gardien** qui tourne à côté et rend inaccessible ce que tu as banni :

| Banni | Ce que fait le gardien |
|---|---|
| **Statuts + Chaînes** | l'onglet « Actus » est recouvert et ne répond plus ; si tu y arrives en glissant, retour sur Discussions ; le lecteur de statuts et les écrans de chaînes se referment aussitôt |
| **Meta AI** | le bouton Meta AI et la discussion Meta AI sont recouverts ; si elle s'ouvre quand même, elle se referme |
| **Communautés** (désactivé par défaut) | l'onglet Communautés est recouvert |

Chaque bannissement s'active ou se désactive dans l'app.

### Pourquoi cette approche
- **Aucun risque pour ton compte** : Minsapp ne touche ni à l'app WhatsApp ni à son réseau. Les WhatsApp modifiés (GBWhatsApp, WhatsApp Plus…) et les clients non officiels font bannir les comptes.
- **Tout WhatsApp reste là**, y compris les fonctions que tu n'as pas bannies.
- **Vie privée** : Minsapp n'a **pas la permission Internet**. Il examine seulement l'écran de WhatsApp pour repérer les éléments bannis, et n'enregistre rien.

Il s'appuie sur le service d'accessibilité d'Android, celui qu'utilisent les lecteurs d'écran.

---

## Installer sur le téléphone

1. Sur le téléphone, ouvre ce lien (connecté à GitHub si le dépôt est privé) et télécharge l'APK :
   **https://github.com/PatrickChoumi/Minsapp/releases/download/latest/minsapp.apk**
2. Ouvre le fichier téléchargé. Android demande d'autoriser l'installation depuis ton navigateur : accepte, puis **Installer**.
3. Ouvre **Minsapp** → **Activer le gardien** → **Minsapp Gardien** → active-le.
   - Si Android affiche **« Paramètre restreint »** : Paramètres › Applications › Minsapp › menu **⋮** › **Autoriser les paramètres restreints**, puis recommence l'étape 3.
4. Ouvre WhatsApp : l'onglet Actus et Meta AI ne sont plus accessibles.

Ménage conseillé, une seule fois, dans WhatsApp : archive (ou supprime) la discussion Meta AI. Si l'option existe chez toi, désactive aussi le bouton Meta AI dans Paramètres › Discussions.

### Mettre à jour
Chaque modification poussée sur GitHub reconstruit l'APK au même lien. Retélécharge-le et installe-le par-dessus : tes réglages sont conservés.

---

## Limites (à connaître)

- **Android uniquement.** Sur iPhone, aucune app ne peut agir sur une autre app.
- **Masquer n'est pas supprimer** : Meta AI existe toujours dans WhatsApp. Ne sont pas couverts : la barre de recherche « Demander à Meta AI ou rechercher » (c'est aussi la recherche normale), les mentions @Meta AI dans les groupes, et les éventuelles suggestions IA dans le champ de saisie.
- Quand une fenêtre passe devant WhatsApp (clavier, volet de notifications, menu, vidéo flottante), les caches s'effacent un instant.
- La couleur des caches est lue sur l'écran de WhatsApp sous Android 11 et plus. Sur les versions plus anciennes, c'est blanc ou sombre selon le thème du téléphone.
- **WhatsApp change régulièrement son interface.** Si un élément banni réapparaît après une mise à jour de WhatsApp, il faut mettre à jour les noms qu'il cherche dans [`core/src/main/kotlin/minsapp/core/WhatsApp.kt`](core/src/main/kotlin/minsapp/core/WhatsApp.kt) (libellés d'onglets, identifiants, noms d'écrans).

---

## Développement

```
core/   règles en Kotlin pur (que masquer, quand revenir en arrière) + tests
app/    l'app Android : service d'accessibilité, caches, écran de réglages
docs/   recherche sur les façons de faire un WhatsApp minimaliste
```

- Tests des règles (sans SDK Android) : `./gradlew -p core test`
- APK : `./gradlew :app:assembleDebug` (avec le SDK Android, par exemple via Android Studio), ou automatiquement par GitHub Actions à chaque push (`.github/workflows/android.yml`).
- L'APK est signé avec `app/minsapp-debug.keystore`, une clé fixe versionnée dans le dépôt, pour que chaque nouvelle version s'installe par-dessus la précédente. C'est adapté à un usage personnel, pas au Play Store.
