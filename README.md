# Projet TinyPet #Cloud #GCP #JavaEE

<strong>Nom du projet :</strong> <i>TinyPet</i> aka [Miage Petition Generator 2021](https://en.wikipedia.org/wiki/Perfection)

<strong>Nom du groupe : <i>Les pitchounous</i></strong>

<strong>Membres :</strong> [Mélissat Mérat](https://github.com/MelissaMerat), [Elyne Merlaud](https://github.com/emerlaud), Gaetan Pellerin & [Samuel Rigaud](https://github.com/s-rigaud)

<strong>But :</strong> Créer un projet Java EE utilisant GCP capable de répondre à ces <i>[exigences](https://docs.google.com/document/d/1X5xcIEPOrEi1BJ5pHEjScY2MLtQkb7rIrgCuLEbEUZQ/edit)</i>

<strong>Url :</strong> <i>https://petitions-frontend.appspot.com/</i> (Le back-end peut mettre un peu de temps à démarrer ~15 secondes)

<strong>Url du backend :</strong> <i>https://petitions-31032021.appspot.com/</i>

<strong>Backend OpenApi :</strong> <i>https://endpointsportal.petitions-31032021.cloud.goog/</i> Accès restreint (consolutez le fichier openapi.json à la base du projet pour plus d'info)

<strong>GitHub du front :</strong> <i>https://github.com/s-rigaud/frontend_cloud</i>
# Authentification

L'authentification se base sur les token OAuth2 de Google.

# Endpoints

| Action                                     | Methode | URL                                    | Connexion requise | Spécificité | Réalisé |
|--------------------------------------------|---------|----------------------------------------|-------------------|-------------|---------|
| Peupler la base de fausses pétitions       | GET     | /petitions/create/fake                 | ❌ |                 | ✔️ |
| Voir les pétitions les plus signées        | GET     | /petitions/top100                      | ❌ | Les 100 pétitions les plus signées triées par date de création | ✔️ |
| Trier les pétitions selon les tags ou le titre | GET     | /petitions/filter                  | ❌ |  | ✔️ |
| Voir le détail d'une pétition              | GET     | /petition?id={id}                      | ❌ |                 | ✔️ |
| Signer une pétition                        | PATCH   | /petition?id={id}                      | ✔️  |                 | ✔️ |
| Créer une pétition                         | POST    | /petitions                             | ✔️  |                 | ✔️ |
| Voir les pétitions que j'ai créé           | GET     | /me/petitions                          | ✔️  | Triées par date | ✔️ |
| Voir les pétitions que j'ai signé          | GET     | /me/signature                          | ✔️  | Triées par date | ✔️ |
| Voir les actions disponibles dans l'API    | GET     | /endpoints                             | ❌ | Retourne l'équivalent de ce tableau au format Json | ✔️ |

# Schema des pétitions dans le Datatstore

## Petition
| Field      | Type       | Usage                                          |
|------------|------------|------------------------------------------------|
| key        | String     | {reversed_creation_timestamp}_{owner}          |
| name       | String     | Nom de la pétition                             |
| owner      | String     | Adresse mail du créateur                       |
| created_at | Date       | Date de création                               |
| signCount  | Integer    | Nombre de vote pour cette pétition             |
| content    | String     | Description du sujet de la pétition            |
| tags       | StringList | Ensemble des tags pour cette pétition          |

Champ additionnel calculé (hors Datastore)
| userAlreadySigned | Boolean | Retourne si l'utilisateur est authentifié et s'il a déjà signé la pétition |
| link              | Map     | Lien vers l'API suivant les principes HATEOAS                              |

![Kind petition](https://github.com/s-rigaud/petition-miage/blob/master/kind_petition.png)

## Petition bucket
| Field   | Type       | Usage                                |
|---------|------------|--------------------------------------|
| key     | String     | {petitionKey}{0..9...}               |
| voters  | StringList | Liste des adresses mails des votants |

Par défaut on créé 10 buckets par pétition et on écrit aléatoirement dans l'un deux

![Kind petition bucket](https://github.com/s-rigaud/petition-miage/blob/master/kind_petition_bucket.png)

## Index

```yaml
indexes:

# For the more signed petition query
#  Query : select * from Petition order by signCount desc, created_at desc

- kind: Petition
  properties:
    - name: signCount
      direction: desc

    - name: created_at
      direction: desc

# For the filtering part using name or tags
# Query : select * from Petition where tags = "patate"
# Query : select * from Petition where tags = "patate" and tag = "artichaut"
# Query : select * from Petition where tags = "patate" and tag = "artichaut" and title >= "Pomme" and title < "Pommf"
- kind: Petition
  properties:

    - name: tags
      direction: asc

    - name: name
      direction: desc
```

# Pages utiles et tutos suivis:

* https://cloud.google.com/endpoints/docs/openapi/get-started-app-engine#before-you-begin

# Limitations

Il reste quelques modifications à apporter pour que l'application scale et fonctionne normalement :

* Ajouter une vérification avant d'écrire dans un bucket  (aujourd'hui on ajoute dans un bucket aléatoire de la pétition sans regarder s'il y a de la place)
* Voir pour optimiser les indexs complexes
* Améliorer la recherche par titre (lowercase, caractères spéciaux, etc ...)

# Recherche par tag
La recherhce par tag fonctionne de la manière suivante : si le mot commence par un # on va chercher les pétitions ayant ce tag. S'il y a plusieurs tags on va chercher celles qui ont les deux. Si l'utilisateur rentre du texte on va chercher toutes les pétitions qui commencent exactement avec le texte saisi. On peut combiner les recherches avec du texte et des tags.