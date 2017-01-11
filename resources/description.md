
## Data model
[Ovation's data model](/public/data_model.png)

## Authentication
Paste your API token into the "API token" field at the top-right of this page


## Getting Started
- Projects and Sources are "top-level" entities in the Ovation data model. 
All other entities (Folders, Files, Revisions, and Activities) belong to at least one Project.

- Files track individual Revisions (versions) of the file and always have at least one "HEAD" Revision, the most recent version.

- Entities have `attributes` that can be modified by PUTing the document with modified `attributes`. The Ovation web application uses some of these attributes (listed in the figure above). Removing these attributes may cause issues for users of the web app. So be nice.

- Entities may be annotated by any user that can read the entity. Annotations include keyword tags, properties (key-value pairs), notes, and time line events.

## Relationships

All relationships between entities in the Ovation data model are bi-directional and many-to-many. Each relationship has a source entity and a target entity. A `rel` specifies the source-to-target relationship an an optional `inverse_rel` specifies the name of the inverse target-to-source relationship. The [data model](/public/data_model.png) shows the `rel` and `inverse_rel` names used by the Ovation web app. You can add additional relationships using this API.

When entities are created by POSTing to the parent, parent/child relationship(s) are automatically created. To add a new relationship, POST a relationship document to the entity's `links/{rel}/relationships` resource. To delete a relationship, send a DELETE to the relationship's`self` link (`/api/v1/relationships/{id}`).
