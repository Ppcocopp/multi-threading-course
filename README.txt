Réalisé par Louise Guérin, Mathis Grange et Paul Grandhomme.

Vous pouvez retrouver les spécifications dans chaques classes abstraites en javadoc et les spécifications au dessus de chaque implémentation en commentaire.

Chacuns des tests proposés sont validés 
(PS : les tests dans le package given.events.queues sont pour la version full event mais peuvent etre adaptés en changeant les classes à appeler pour valider les tests  de la version mixed)

Malgré le fait que les tests de la couche full event sont validés, nous savons qu'il persiste un bug que nous n'avons pas encore réussi à résoudre :
- Mettons que depuis la couche MessageQueue nous voulons envoyer un message de taille 20 octets puis un 2ème de 20 octets aussi.
- A la couche Channel, nous nous retrouvons avec 2 WriteRequests en file d'attente [WriteRequest 1 : érire 20 octets][WriteRequest 2 : érire 20 octets]
- L'execution du 1er commence mais la méthode write de Channel réussit à écrire que 5 octets, il en reste donc 15 à envoyer donc à la couche MessageQueue on refait une requête d'écriture pour finir le message.
- On se retrouve avec cette liste d'attente : [WriteRequest 2 : érire 20 octets][WriteRequest 1 : érire 15 octets]
- Le 1er message sera donc coupé en 2