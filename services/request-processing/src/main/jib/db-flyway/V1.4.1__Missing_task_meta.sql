alter table requestor_identity
add column taskname varchar;

alter table ongoingtask
add column invitationUri varchar;
