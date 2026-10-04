# Minsapp — Les (1001) manières de faire un WhatsApp minimaliste

> Objectif : une messagerie type WhatsApp **sans IA, sans Statuts, sans Chaînes**.
> Recherche réalisée le 4 octobre 2026. Les prix et statuts de projets bougent vite : revérifier avant de s'engager.

---

## 0. En bref

Une seule question décide de presque tout :

> **Est-ce que Minsapp doit pouvoir parler aux contacts WhatsApp existants, ou est-ce un nouveau réseau ?**

| Si tu veux… | Meilleure voie | Effort |
|---|---|---|
| Utiliser WhatsApp toi-même, sans le superflu | Famille A : réglages + extension navigateur (ou passer à Signal) | Quelques minutes |
| Un client « WhatsApp épuré » qui parle **légalement** aux utilisateurs WhatsApp | Famille B : interopérabilité DMA (Europe uniquement, 1:1 seulement aujourd'hui) | Très élevé (il faut d'abord ton propre service E2EE) |
| Un **nouveau** messager minimaliste, rapidement et sérieusement | Famille C : fork d'un client open source (recommandé : **FluffyChat + Matrix**) | Quelques semaines |
| Un nouveau messager avec un contrôle total | Famille F : tout construire (Flutter/KMP + backend Elixir/Go + Signal Protocol ou MLS) | Plusieurs mois |
| Un prototype pour valider l'idée cette semaine | Famille D : SDK de chat managé (Stream, CometChat…) | Quelques jours |

À éviter : les APK modifiés (GBWhatsApp, WhatsApp Plus) et les clients non officiels construits sur le protocole WhatsApp rétro-ingéniéré pour un **produit** : bannissements de comptes, violation des CGU.

---

## 1. Définir « minimaliste » (le périmètre)

**À garder (cœur de WhatsApp) :**
- discussions 1:1 et groupes ;
- texte, photos, vidéos, fichiers, **messages vocaux** ;
- accusés de réception (envoyé / reçu / lu), « en train d'écrire » ;
- chiffrement de bout en bout (E2EE) ;
- notifications push ;
- plus tard : appels audio/vidéo, multi-appareils, sauvegarde.

**À retirer :**
- Meta AI et toute fonction IA (résumés, réponses suggérées, génération d'images…) ;
- Statuts / Stories (onglet « Actus ») ;
- Chaînes, Communautés ;
- accessoires : WhatsApp Business, paiements, boutique de stickers, événements, sondages (au choix).

Retirer ces fonctions simplifie énormément l'architecture : pas de diffusion « un vers des millions » (Chaînes), pas de contenus éphémères publics (Statuts), pas d'infrastructure IA.

---

## 2. Famille A — Garder WhatsApp, cacher le superflu (zéro ou très peu de code)

### A1. Les réglages officiels (mobile)
- **Meta AI ne peut pas être supprimé**, seulement caché : `Paramètres → Discussions → Afficher le bouton Meta AI` (déploiement progressif, pas encore visible chez tout le monde), suppression/archivage de la discussion Meta AI.
- **Confidentialité avancée des discussions** (par discussion ou groupe) : empêche notamment de mentionner @Meta AI dans cette discussion.
- L'onglet **Actus (Statuts + Chaînes) ne peut pas être désactivé** ; on peut seulement masquer les statuts de chaque contact et ne suivre aucune chaîne.

### A2. WhatsApp Web + extension / userscript (ordinateur)
- **Hide WhatsApp Web Buttons** (Chrome/Firefox) : cache les boutons Statut, Chaînes, Communautés.
- **No AI WhatsApp** : cache la discussion Meta AI et le bouton « Ask Meta AI ».
- **Collapse WhatsApp** (userscript Tampermonkey) : masque Chaînes/barre latérale au raccourci clavier.
- Variante « fais-le toi-même » : une petite extension (ou un wrapper Tauri/Electron chargeant `web.whatsapp.com`) qui injecte du **CSS** pour masquer ces éléments. C'est l'approche la plus simple pour un « Minsapp » personnel sur ordinateur.
  - Risque : faible (simple masquage côté navigateur) mais fragile, car chaque mise à jour de WhatsApp Web peut casser les sélecteurs CSS.

### A3. APK modifiés (GBWhatsApp, WhatsApp Plus…) — ❌ à exclure
WhatsApp suspend ces comptes, temporairement puis définitivement. En plus, ces APK viennent de sources inconnues, avec un risque de malware.

### A4. Ton propre client sur le protocole WhatsApp non officiel — ⚠️ seulement pour expérimenter
- Bibliothèques : **Baileys** (TypeScript, WebSocket), **whatsmeow** (Go), **whatsapp-web.js** (Puppeteer).
- Principe : ton client se connecte comme un « appareil lié » à ton compte, et tu dessines l'interface que tu veux (sans IA, Statuts ni Chaînes).
- Problèmes : **violation des CGU**, risque de **bannissement définitif** du numéro sans préavis. Si le client tourne sur un serveur, tes clés de chiffrement s'y trouvent aussi. Ce n'est pas une base pour un produit public.

### A5. Pont Matrix vers WhatsApp (Beeper / mautrix-whatsapp)
- **mautrix-whatsapp** (open source, maintenu par Beeper) relie ton compte WhatsApp à Matrix. Tu lis et écris tes discussions WhatsApp depuis **n'importe quel client Matrix** minimaliste, ou depuis Beeper.
- Avantage : une boîte de réception unique et épurée (WhatsApp + Signal + Telegram…).
- Inconvénient : même fondement non officiel que A4 (le pont s'appuie sur whatsmeow), donc risque théorique pour le compte. Usage personnel uniquement.

### A6. API WhatsApp Business (Cloud API) — ❌ hors sujet
Prévue pour les entreprises (modèles de messages, facturation au message), pas pour discuter entre particuliers. Depuis le 15 janvier 2026, Meta y interdit aussi les chatbots généralistes.

### A7. Ne rien construire : utiliser Signal
Signal est déjà, de fait, un « WhatsApp sans IA ni Chaînes ». Les Stories y sont désactivables dans les réglages. C'est la réponse la plus simple si le besoin est personnel.

---

## 3. Famille B — La voie officielle : l'interopérabilité DMA (Union européenne)

C'est **la seule voie légale** pour qu'une application tierce discute avec des utilisateurs WhatsApp.

**État en octobre 2026 :**
- Le **Digital Markets Act (art. 7)** oblige Meta à rendre WhatsApp interopérable avec les autres messageries. Meta a publié une **offre de référence** : spécifications techniques et contrat type.
- Les « discussions tierces » ont été lancées le **14 novembre 2025** avec deux partenaires : **BirdyChat** et **Haiket**. BirdyChat est passé en lancement ouvert dans toute l'Europe en **mai 2026**.
- Ce qui marche : **1:1 uniquement**, avec texte, images, messages vocaux, vidéos et fichiers. Les **groupes** viendront « quand les partenaires seront prêts ». Pour les **appels**, la feuille de route de Meta vise 2027.
- Limites : seulement les comptes WhatsApp liés à un **numéro européen**, seulement sur les **applications mobiles** (pas le Web ni l'ordinateur), et l'utilisateur WhatsApp doit **activer** l'option (opt-in).

**Conditions côté fournisseur tiers :**
- signer l'accord de l'offre de référence avec Meta ;
- utiliser le **Signal Protocol**, ou prouver des garanties de sécurité équivalentes ;
- encapsuler les messages chiffrés dans des « stanzas » XML ; les médias transitent chiffrés via un proxy Meta ;
- le DMA impose à Meta de répondre à une demande raisonnable **sous 3 mois**, gratuitement ;
- il faut **être ou devenir un vrai service de messagerie opérant dans l'UE** : l'interop se branche sur ton propre réseau, elle ne le remplace pas.

**Conséquence pour Minsapp :** c'est un objectif de **phase 2**. Pour le garder possible, il faut dès le départ un E2EE compatible Signal Protocol (Famille F) ou une couche capable de le parler. Un fork Matrix (Olm/Megolm) demanderait un développement spécifique.

---

## 4. Famille C — Partir d'un messager open source existant (fork / rebrand / configuration)

On prend un client qui fonctionne déjà, on **retire** ce qu'on ne veut pas et on change le nom et le design. C'est souvent le meilleur rapport effort/résultat.

| Projet | Protocole / réseau | Licence | Identité | Auto-hébergement | Remarques pour Minsapp |
|---|---|---|---|---|---|
| **FluffyChat** | Matrix | AGPL-3.0 | identifiant Matrix (+ e-mail/téléphone possibles) | homeserver Matrix | **Flutter** : une seule base de code pour Android, iOS, web et ordinateur. Push sans Google via UnifiedPush. Excellente base à épurer. |
| **Element X** | Matrix | AGPL-3.0 (le SDK `matrix-rust-sdk` est sous Apache-2.0) | idem | idem | Natif Swift + Kotlin (2 bases de code). On peut aussi écrire **sa propre interface** sur `matrix-rust-sdk`. |
| **Conversations / Quicksy** | XMPP | GPL-3.0 | **Quicksy = numéro de téléphone + découverte automatique des contacts** (le plus « WhatsApp-like ») | Prosody, ejabberd, Snikket | Android uniquement ; pour iOS il faut Monal ou Siskin. E2EE OMEMO. |
| **Snikket** | XMPP | Apache-2.0 / GPL | invitations | serveur « tout-en-un » | Pensé pour familles et petits groupes, auto-hébergement simple. |
| **Signal / Molly** | Signal | AGPL-3.0 | téléphone (+ pseudo) | **Signal-Server difficile à auto-héberger** (service de stockage mal documenté, nombreuses dépendances) | Un fork rebrandé ne doit pas utiliser les serveurs de Signal. Crypto de référence. |
| **Delta Chat / ArcaneChat** | e-mail (chatmail) | cœur Rust réutilisable | adresse générée, pas de téléphone | relais **chatmail** légers | ArcaneChat est déjà un fork compatible. E2EE imposé, décentralisé. |
| **SimpleX Chat** | SMP | AGPL-3.0 | **aucun identifiant** | serveurs SMP/XFTP | Très privé. Voir l'alerte Apple sur les « discussions anonymes » (§9). |
| **Session** | réseau Session (routage en oignon) | GPL-3.0 | ID aléatoire | — | Pas de téléphone, réseau propre. |
| **Tinode** | protocole propre (JSON/WebSocket, gRPC) | serveur GPL-3.0, clients **Apache-2.0** | téléphone/e-mail/login | serveur Go | Se présente comme un « WhatsApp/Telegram open source ». Clients Android, iOS et web prêts. **E2EE : statut ambigu (« prévu » dans le README), à vérifier.** |
| **Wire** | MLS | AGPL-3.0 | e-mail/pseudo | lourd (Kubernetes, Cassandra, Postgres, Redis) | Plutôt orienté entreprise. |
| **Jami** | P2P (OpenDHT) | GPL-3.0 | clé | pas de serveur central | Pair-à-pair pur. |

**Cas particulier : Telegram via TDLib ou un fork (Nekogram, Nagram, Telegram FOSS)**
- Telegram **autorise** les clients tiers : il faut ton propre `api_id`, l'app doit indiquer clairement qu'elle utilise l'API Telegram, et les fonctions de base (statuts « lu », « en ligne », messages éphémères…) ne doivent pas être cassées.
- On peut donc faire un **client Telegram minimaliste**, légalement, en masquant Stories, Chaînes, bots et IA. Nekogram sait déjà masquer les Stories.
- Limite majeure : les discussions Telegram **ne sont pas chiffrées de bout en bout par défaut** (seuls les « chats secrets » 1:1 le sont).

---

## 5. Famille D — SDK de chat managé (SaaS)

Un service clé en main (serveur, base de données, temps réel, souvent un kit d'interface « WhatsApp-like ») sur lequel tu construis l'application.

| Service | Gratuit | Payant (ordre de grandeur 2026) |
|---|---|---|
| **Stream Chat** | 1 000 MAU, gratuit en permanence (offre Build) | ~399 $/mois à l'année (499 $ au mois) pour 10 000 MAU |
| **Sendbird** | 100 MAU (Developer) | Starter dès ~399 $/mois |
| **CometChat** | 100 MAU | Basic ~87 $/mois à l'année (~1 000 MAU) ; Scale ~1 249 $/mois |
| Autres | Tencent Cloud Chat, ZEGOCLOUD, Agora Chat, PubNub, Ably… | variable |

- ✅ Prototype en quelques jours, groupes, médias, accusés de lecture et push inclus.
- ❌ En général **pas de vrai E2EE** (le fournisseur peut lire les messages), dépendance forte au fournisseur, coût proportionnel au nombre d'utilisateurs. Certains poussent des fonctions IA : il suffit de ne pas les activer.

---

## 6. Famille E — Backend-as-a-Service + ton propre code

Tu écris l'application et la logique de chat ; le BaaS fournit la base de données, l'authentification, le stockage et le temps réel.

- **Firebase** : Firestore + Auth (dont SMS) + FCM. Le plus mature et le plus rapide pour démarrer ; modèle NoSQL.
- **Supabase** : Postgres + Realtime + Auth + Storage, open source. Paquets Flutter existants pour l'E2EE :
  - `supabase_chat_e2ee` : Signal Protocol (Double Ratchet), **GPL-3.0**, ce qui t'oblige à publier ton code ;
  - `supabase_chat_seal` : MIT, « sealed box » X25519 + AES-256-GCM, mais **sans confidentialité persistante** (forward secrecy).
- **PocketBase** : un seul binaire Go + SQLite + temps réel. Parfait pour un petit réseau auto-hébergé (famille, association).
- **Appwrite** (auto-hébergeable sous Docker), **Convex** (TypeScript, requêtes temps réel par défaut, très adapté au chat), **InstantDB**, **Nhost**.

Dans tous les cas, l'E2EE est **ta** responsabilité : le BaaS ne stocke que du texte chiffré.

---

## 7. Famille F — Tout construire (contrôle total)

C'est le plus de travail, mais aussi la seule voie qui garde toutes les portes ouvertes, y compris l'interop DMA. Voici les couches, avec les options pour chacune.

### 7.1 Application cliente
| Option | Pour | Contre |
|---|---|---|
| **Flutter** | une base de code pour mobile, web et ordinateur ; écosystème chat riche | interface non native |
| **React Native / Expo** | JS/TS, l'option la plus répandue (~43 % des devs mobiles en 2026) | la crypto native passe par des modules natifs |
| **Kotlin Multiplatform + Compose Multiplatform** | logique partagée et UI native possible ; adoption en forte hausse (~23 %) | écosystème plus jeune |
| **Natif Swift + Kotlin** | meilleure qualité possible (c'est le choix de WhatsApp et Signal) | deux applications à écrire |
| **PWA** (web installable) | zéro store ; Web Push fonctionne sur iOS pour les apps ajoutées à l'écran d'accueil | expérience et notifications moins fiables |

### 7.2 Transport temps réel
WebSocket maison, **XMPP** (WhatsApp est né sur ejabberd), **MQTT**, Phoenix Channels (Elixir), Socket.IO, **Centrifugo**, flux gRPC.

### 7.3 Backend
- **Erlang/Elixir** : l'ADN de WhatsApp (Erlang sur FreeBSD, environ 2 M de connexions par serveur, une cinquantaine d'ingénieurs pour des centaines de millions d'utilisateurs). Serveurs prêts à l'emploi : **ejabberd**, **MongooseIM** (6.5 sortie en janvier 2026).
- **Go** (Tinode, LiveKit), **Rust**, **Node/TypeScript**, **Kotlin/Ktor**.

### 7.4 Données
- Serveur : **Postgres** pour commencer ; ScyllaDB/Cassandra si l'échelle l'exige. Médias dans un stockage objet compatible S3, **chiffrés côté client**.
- Appareil : SQLite (les messages vivent surtout sur le téléphone, comme sur WhatsApp).

### 7.5 Chiffrement de bout en bout (ne jamais l'inventer soi-même)
| Bibliothèque | Protocole | Licence | Remarque |
|---|---|---|---|
| **libsignal** | Signal Protocol (X3DH/PQXDH + Double Ratchet) | **AGPL-3.0** | bindings officiels Java, Swift, TypeScript (cœur Rust) ; **requis pour l'interop WhatsApp** |
| **OpenMLS** | **MLS (RFC 9420)**, standard IETF | MIT | meilleur choix pour les **groupes** ; exemples React Native (Margelo) ; utilisé par Wire |
| **vodozemac** | Olm/Megolm (Matrix) | Apache-2.0 | si tu restes compatible Matrix |

Pour les notifications avec E2EE : envoyer des push **vides** (« réveil ») ou chiffrées, puis déchiffrer sur l'appareil (Notification Service Extension sur iOS). C'est l'approche de Signal.

### 7.6 Notifications push
- iOS : **APNs obligatoire**.
- Android : **FCM**, ou **UnifiedPush/ntfy** pour les téléphones sans Google (F-Droid).

### 7.7 Appels (phase ultérieure)
- 1:1 : WebRTC en pair-à-pair + serveur **coturn** (TURN).
- Groupes : **LiveKit** (Apache-2.0, Go, le standard de fait en 2026), mediasoup (le plus efficace en CPU), Jitsi (clé en main).
- Auto-héberger devient nettement moins cher que le payant à la minute à partir d'environ 15 M de minutes par mois.

### 7.8 Identité et découverte des contacts
| Méthode | Pour | Contre |
|---|---|---|
| **Numéro + code SMS** (comme WhatsApp) | découverte automatique via le carnet d'adresses | coût (Twilio Verify : 0,05 $ par vérification + le SMS) ; **fraude au « SMS pumping »** ; problème de vie privée (hacher les numéros ne suffit pas) |
| **Pseudo + lien/QR d'invitation** | gratuit, privé, simple | pas de découverte automatique |
| **E-mail** / **passkeys** | moins cher que le SMS | moins « WhatsApp » |

Suggestion : pseudo + invitation par lien/QR au départ, numéro de téléphone optionnel plus tard.

---

## 8. Famille G — Décentralisé, pair-à-pair, hors ligne

- **Nostr** : messages privés (NIP-17) ; **White Noise** = MLS sur Nostr (protocole *Marmot*, audits publiés en mars et avril 2026, v0.3.0 en temps réel).
- **bitchat** : maillage Bluetooth LE (relais multi-sauts) + Nostr pour l'internet ; versions 1.6 et 1.7 en juillet 2026.
- **Briar** : maintenant en **mode maintenance** (correctifs de sécurité seulement).
- **Jami** (OpenDHT), **libp2p**, Matrix P2P (expérimental).
- Intéressant pour une **niche** (zones sans réseau, militants…). Pour un usage grand public type WhatsApp, l'expérience reste plus fragile : batterie, Android qui tue les tâches de fond, synchronisation.

---

## 9. Famille H — No-code / low-code

FlutterFlow, Bubble, Adalo, Thunkable, etc. proposent des modèles de chat (souvent sur Firebase ou Supabase). C'est pratique pour une maquette cliquable. En revanche, pas d'E2EE sérieux, et les limites arrivent vite pour un vrai messager.

---

## 10. Contraintes communes à toutes les voies

- **Stores**
  - Apple (règle 1.2, contenu généré par les utilisateurs) exige un filtrage du contenu choquant, un **signalement**, un **blocage** d'utilisateur et un contact publié.
  - Depuis **février 2026**, Apple peut retirer sans préavis les apps de « discussion aléatoire ou anonyme ». Un messager entre contacts connus n'est pas concerné, mais il vaut mieux éviter de se présenter comme « anonyme ».
  - Google Play a durci ses règles pour ce type d'apps (échéance au 26 août 2026).
- **Licences** : forker de l'AGPL/GPL (FluffyChat, Element X, Signal, SimpleX…) oblige à **publier ton code** sous la même licence. La compatibilité GPL ↔ App Store est juridiquement discutée quand tu n'es pas titulaire des droits du code d'origine.
- **RGPD** : minimiser les données (métadonnées, carnet d'adresses), hébergement dans l'UE de préférence.
- **Nom et marque** : ne pas utiliser « WhatsApp » ni son logo. Vérifier « Minsapp » sur INPI/EUIPO.
- **Effet réseau** : c'est le vrai défi. Une messagerie ne vaut que par les gens qui y sont. D'où l'intérêt stratégique de l'interop DMA, ou d'un public cible précis (famille, école, association, entreprise).

---

## 11. Matrice de décision

| Approche | Parle aux contacts WhatsApp | E2EE | Effort | Coût de départ | Risque principal |
|---|---|---|---|---|---|
| A1/A2 Réglages + extension | ✅ (c'est WhatsApp) | ✅ | ⭐ | 0 | Meta AI reste dans l'app mobile |
| A4/A5 Client non officiel / pont | ✅ | ⚠️ | ⭐⭐ | 0 | **Bannissement, CGU** |
| B Interop DMA | ✅ (UE, 1:1) | ✅ | ⭐⭐⭐⭐⭐ | élevé | Conformité, accord Meta, UE seulement |
| C Fork FluffyChat/Matrix | ❌ (sauf pont) | ✅ | ⭐⭐ | serveur ~5–20 €/mois | AGPL, complexité Matrix |
| C Fork Quicksy/XMPP | ❌ | ✅ | ⭐⭐⭐ (Android + iOS séparés) | faible | Deux apps différentes |
| C Client Telegram minimal | ❌ (contacts Telegram ✅) | ⚠️ pas par défaut | ⭐⭐ | 0 | Règles de l'API Telegram |
| D SDK managé | ❌ | ❌ en général | ⭐ | 0 → 400 $+/mois | Dépendance, coût, pas d'E2EE |
| E BaaS + code | ❌ | à faire soi-même | ⭐⭐⭐ | faible | Crypto maison |
| F Tout construire | ❌ (✅ plus tard via DMA) | ✅ (libsignal/MLS) | ⭐⭐⭐⭐⭐ | moyen | Temps, sécurité |

---

## 12. Recommandation pour Minsapp

1. **Trancher la question clé** (§0). Si le besoin est personnel : Famille A, ou simplement Signal.
2. **Si Minsapp est un produit, un nouveau réseau** : *Voie recommandée*
   - **Fork de FluffyChat** (Flutter, AGPL-3.0) sur **ton propre homeserver Matrix** : Synapse via *Element Server Suite Community* pour 1 à 100 utilisateurs, ou Tuwunel / Continuwuity (Rust, plus léger).
   - Retirer : Spaces, annuaire de salons publics, fonctions avancées. Ajouter : un écran d'accueil « liste de discussions » façon WhatsApp, l'invitation par lien/QR.
   - Tu récupères gratuitement : E2EE, groupes, médias, messages vocaux, multi-appareils, push (y compris UnifiedPush), et plus tard les appels (MatrixRTC/Element Call). Les ponts restent possibles.
3. **Si tu veux le contrôle total et viser l'interop WhatsApp (UE)** : Famille F, avec **Flutter ou KMP + backend Elixir (ou Go) + libsignal**. C'est un projet de plusieurs mois.
4. **Pour valider l'idée en une semaine avant tout** : maquette sur **Stream Chat** (1 000 MAU gratuits), sans E2EE, à jeter ensuite.

---

## Sources

- Interop DMA : [Engadget](https://www.engadget.com/apps/whatsapp-enables-interoperability-with-two-other-messengers-in-the-eu-140000835.html), [MacRumors](https://www.macrumors.com/2025/11/14/whatsapp-third-party-chat-support-eu/), [CyberInsider](https://cyberinsider.com/whatsapp-enables-messaging-interoperability-with-birdychat-and-haiket/), [Privacy Guides](https://www.privacyguides.org/news/2025/11/18/whatsapp-rolls-out-messaging-interoperability-for-europeans/), [Meta Newsroom](https://about.fb.com/news/2025/11/messaging-interoperability-whatsapp-enables-third-party-chats-for-users-in-europe/), [PPC Land (BirdyChat)](https://ppc.land/birdychat-opens-to-50k-waitlist-with-whatsapp-interoperability-in-europe/), [TechCrunch (Signal Protocol)](https://techcrunch.com/2024/03/06/to-comply-with-dma-whatsapp-and-messenger-will-become-interoperable-via-signal/), [Android Police](https://www.androidpolice.com/whatsapp-and-messenger-will-be-able-to-connect-with-interoperable-third-party-messaging-apps/), [Commission européenne – interop messagerie](https://digital-markets-act.ec.europa.eu/messaging-interoperability_en), [DMA art. 7](https://www.springlex.eu/en/packages/dma/dma-regulation/article-7/)
- Masquer IA/Statuts/Chaînes : [Forbes](https://www.forbes.com/sites/zakdoffman/2025/05/14/how-to-remove-meta-ai-from-whatsapp-you-can-do-this-now/), [Techpoint Africa](https://techpoint.africa/guide/remove-meta-ai-whatsapp/), [Hide WhatsApp Web Buttons (Firefox)](https://addons.mozilla.org/sr/firefox/addon/hide-wpp-web-buttons/), [Collapse WhatsApp (Greasy Fork)](https://greasyfork.org/cs/scripts/511469-collapse-whatsapp)
- Clients non officiels et mods : [Zylos – WhatsApp API 2026](https://zylos.ai/research/2026-01-26-whatsapp-api-automation), [Xataka (GB/Plus)](https://www.xataka.com.mx/aplicaciones/whatsapp-suspendera-previo-aviso-cuentas-quienes-usen-apps-no-oficiales-gb-whatsapp-whatsapp-plus-mira), [Beeper bridges](https://developers.beeper.com/bridges/index.md)
- Open source : [FluffyChat (Snapcraft)](https://snapcraft.io/fluffychat), [Element – AGPL](https://element.io/blog/sustainable-licensing-at-element-with-agpl), [ESS Community](https://app.media.ccc.de/v/matrix-conf-2025-73143-getting-started-with-element-server-suite-community), [Serveurs Matrix](https://www.matrix.org/ecosystem/servers/), [Conversations/Quicksy](https://en.wikipedia.org/wiki/Conversations_(software)), [Quicksy F-Droid](https://f-droid.org/hr/packages/im.quicksy.client/), [Signal-Server auto-hébergé (SoftwareMill)](https://softwaremill.com/can-you-self-host-the-signal-server/), [Delta Chat/chatmail (YunoHost)](https://forum.yunohost.org/t/chatmail-relay-server-for-delta-chat/40341), [Tinode](https://pkg.go.dev/github.com/tinode/chat), [Wire](https://www.networkworld.com/article/962396/instant-messaging-service-wire-open-sources-its-server-code.html), [SimpleX](https://www.opensourcealternatives.to/item/simplex-chat), [Telegram API ToS](https://core.telegram.org/api/terms), [TDLib](https://core.telegram.org/tdlib), [Nagram](https://github.com/NextAlone/Nagram)
- SDK et BaaS : [Comparatif SDK 2026 (TRTC)](https://trtc.io/blog/details/sendbird-alternative-2026-free-tier-comparison), [Prix CometChat](https://hackceleration.com/labs/cometchat-pricing), [Comparatif BaaS 2026](https://www.youngju.dev/blog/culture/2026-05-14-baas-comparison-2026-supabase-firebase-pocketbase-appwrite-convex-instantdb-deep-dive), [supabase_chat_e2ee](https://pub.dev/packages/supabase_chat_e2ee), [supabase_chat_seal](https://pub.dev/packages/supabase_chat_seal)
- Construire : [Architecture WhatsApp](https://singhajit.com/whatsapp-scaling-secrets/), [MongooseIM](https://www.linuxlinks.com/mongooseim-mobile-messaging-platform-performance-scalability), [OpenMLS](https://lib.rs/crates/openmls), [OpenMLS en React Native (Margelo)](https://margelo.com/blog/building-e2e-encrypted-group-messaging-with-openmls), [libsignal](https://github.com/SignalApp/libsignal-client), [UnifiedPush (F-Droid)](https://f-droid.org/2022/12/18/unifiedpush.html), [WebRTC 2026](https://www.youngju.dev/blog/culture/2026-05-14-webrtc-media-infrastructure-2026-livekit-pion-daily-100ms-mediasoup-whip-whep-deep-dive), [RN vs Flutter vs KMP 2026](https://dev.to/techqware/kotlin-multiplatform-vs-flutter-vs-react-native-which-cross-platform-framework-should-you-choose-1k79), [Twilio Verify prix (Authgear)](https://www.authgear.com/post/twilio-verify-pricing-and-alternatives)
- Décentralisé : [White Noise audits](https://leastauthority.com/blog/audit-of-white-noise-marmot-protocol-review/), [bitchat 2026](https://dev.to/creeta/two-bitchat-releases-in-48-hours-moved-the-offline-mesh-stack-44fn)
- Stores : [Apple App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/), [9to5Mac (fév. 2026)](https://9to5mac.com/2026/02/06/apple-says-random-or-anonymous-chat-apps-no-longer-welcome-on-the-app-store/), [MediaNama](https://www.medianama.com/2026/02/223-apple-app-store-guidelines-anonymous-chat-apps/), [Google Play août 2026](https://ecorpit.com/google-play-anonymous-chat-age-restricted-policy-august-2026/)
