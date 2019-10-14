
/* update_ddl_160422 required! */

/* uncomment if not present */
/*
ALTER TABLE NET_CENTER ADD 
PUBKEY BLOBOBJ;

ALTER TABLE NET_CENTER ADD 
USRID "INTEG-1";

ALTER TABLE LC_CAD ADD 
CV_CT SHORT;

CREATE DOMAIN DTNOW
 AS Timestamp
 DEFAULT 'NOW';
ALTER TABLE LC_MAIN ADD PATEXTID1 VCHAR255;
ALTER TABLE LC_MAIN ADD EXTID1DESC VCHAR255;
ALTER TABLE LC_MAIN ADD PATEXTID2 VCHAR255;
ALTER TABLE LC_MAIN ADD EXTID2DESC VCHAR255;
ALTER TABLE LC_MAIN ADD PRSID INTEGER;

ALTER TABLE LC_CAD ADD CHANGED DTNOW;
ALTER TABLE LC_CAD ADD VALIDTO Timestamp;

ALTER TABLE EHR_MAIN ALTER PATID TO PRSID;

ALTER TABLE EHR_COMP ALTER PATID TO PRSID;

*/
DROP TABLE USR_ADR;

ALTER TABLE LC_MAIN ADD PATIDISPRSID "BOOLEAN";

UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = 1
WHERE RDB$FIELD_NAME = 'CV_CT' AND RDB$RELATION_NAME = 'LC_CAD';

ALTER DOMAIN STRINGIP
  SET DEFAULT '0.0.0.0'
  -- | DROP DEFAULT
  -- | ADD [CONSTRAINT] CHECK (condition)
  -- | DROP CONSTRAINT
  -- | new_name
  TYPE varchar(15);

CREATE TABLE PRS_MAIN
(
    prsid INTEGER NOT NULL,
    firstname VARCHAR(64) DEFAULT NULL,
    lastname VARCHAR(64) DEFAULT NULL,
    dob DATE,
    --9=new, 1=active, 9=deactivated/inactive
    status SHORT DEFAULT 0,
    pkidata BLOB SUB_TYPE 0 DEFAULT NULL,
    registered TIMESTAMP,
    registeredby INTEGER,
    changed TIMESTAMP,
    changedby INTEGER,
    CONSTRAINT PK_PRS_MAIN
    PRIMARY KEY (prsid)
);
CREATE TABLE PAT_MAIN
(
    patid INTEGER NOT NULL,
    info varchar(255) default '',
    --date of death
    dod Date DEFAULT null,
    CONSTRAINT PK_PAT_MAIN
      PRIMARY KEY (patid)
);
commit;

alter table PAT_MAIN
add constraint FK_PAT_MAIN_PRS_MAIN
foreign key (PATID) 
references PRS_MAIN (PRSID)
-- on update NO ACTION 
;
commit;

CREATE TABLE PRS_RELSHIP
(
  RELID INTEG DEFAULT 0 NOT NULL,
  PRSID1 INTEG DEFAULT 0 NOT NULL,
  PRSID2 INTEG DEFAULT 0 NOT NULL,
  INFO VCHAR128,
  RELTYPE SHORT DEFAULT 0,
  CONSTRAINT PK_PRS_RELSHIP PRIMARY KEY (RELID),
  CONSTRAINT UNQ_PRS_RELSHIP_OUTER UNIQUE (RELID,PRSID1,PRSID2)
);
DROP TABLE USR_RELSHIP;
alter table PAT_MAIN
add constraint FK_PAT_PRS
foreign key (PATID) 
references PRS_MAIN (PRSID)
-- on update NO ACTION 
;
CREATE GENERATOR GEN_PRSID;
SET GENERATOR GEN_PRSID TO 100;

SET TERM ^ ;
CREATE TRIGGER PRS_MAIN_BI FOR PRS_MAIN
ACTIVE BEFORE INSERT POSITION 0
AS 
BEGIN 
	IF (NEW.PRSID IS NULL) THEN NEW.PRSID = GEN_ID(GEN_PRSID,1);
END^
SET TERM ; 
commit;

alter table USR_MAIN
add constraint FK_USR_PRS
foreign key (USRID) 
references PRS_MAIN (PRSID)
on update NO ACTION
on delete NO ACTION
;
DROP GENERATOR GEN_USRRELID;
CREATE GENERATOR GEN_RELID;
SET GENERATOR GEN_RELID TO 0;
SET TERM ^ ;
CREATE TRIGGER PRS_RELSHIP_BI FOR PRS_RELSHIP
ACTIVE BEFORE INSERT POSITION 0
AS 
BEGIN 
	 
	IF (NEW.RELID IS NULL) THEN NEW.RELID = GEN_ID(GEN_RELID,1);
END^
SET TERM ; ^
commit;

SET TERM ^ ;
ALTER TRIGGER USR_MAIN_BI ACTIVE
BEFORE INSERT POSITION 0
AS
BEGIN
  IF(NEW.USRID IS NULL) THEN NEW.USRID=GEN_ID(GEN_PRSID,1);
END^
SET TERM ; ^
commit;

SET TERM ^ ;
ALTER PROCEDURE LIST_USERS (
    SEARCH Varchar(64) )
RETURNS (
    USRID Integer,
    USRNAME Varchar(64),
    -- SURNAME Varchar(64),
    LASTNAME Varchar(64),
    FIRSTNAME Varchar(64),
    REQMAIL Varchar(64),
    STATUS Integer,
    LASTLOGIN Timestamp )
AS
begin
  if((SEARCH is null)OR(SEARCH = '')) then begin
    for SELECT U.USRID,U.USRNAME,P.LASTNAME,P.FIRSTNAME,U.REQMAIL,U.STATUS
      FROM USR_MAIN U, PRS_MAIN P
      WHERE U.USRID=P.PRSID
      into :USRID, :USRNAME, :LASTNAME, :firstname, :reqmail, :STATUS
    do begin
      lastlogin=null;
      SELECT max(LOGINDTS) FROM ACC_USRLOG WHERE USRID=:USRID
        into :lastlogin;
      suspend;
    end
  end
  else begin
    for SELECT U.USRID,U.USRNAME,P.LASTNAME,P.FIRSTNAME,U.REQMAIL,U.STATUS
      FROM USR_MAIN U, PRS_MAIN P
      WHERE U.USRID=P.PRSID
        AND U.USRNAME CONTAINING :SEARCH
      into :USRID, :USRNAME, :LASTNAME, :firstname, :reqmail, :STATUS
      do begin
      lastlogin=null;
      SELECT max(LOGINDTS) FROM ACC_USRLOG WHERE USRID=:USRID
        into :lastlogin;
      suspend;
    end
  end
end^
SET TERM ; ^
commit;

GRANT EXECUTE
 ON PROCEDURE LIST_USERS TO  SYSDBA;
ALTER TABLE USR_MAIN DROP SURNAME;
ALTER TABLE USR_MAIN DROP FIRSTNAME;

CREATE TABLE PRS_CONTACT
(
  PRSID INTEG DEFAULT 0 NOT NULL,
  ADRID INTEG DEFAULT 0 NOT NULL,
  ISBILLING "BOOLEAN" DEFAULT 0,
  OPTID INTEG DEFAULT 0,
  CHANGED Date DEFAULT 'NOW',
  CHANGEDUSRID "INTEGER" DEFAULT 0,
  CONSTRAINT PK_PRS_ADR PRIMARY KEY (PRSID,ADRID)
);
ALTER TABLE PRS_MAIN ADD GENDER SHORT;
ALTER TABLE PAT_MAIN ADD SOCIALID VCHAR64;
ALTER TABLE PAT_MAIN ADD SOCIALIDTYPE SHORT;
ALTER TABLE NET_ZONES ALTER VPNIP TYPE Varchar(15);
ALTER TABLE NET_ZONES ALTER HOSTID TYPE VCHAR32;
ALTER TABLE NET_ZONES ADD RESPORG_ADRID "INTEGER" NOT NULL;
commit;















