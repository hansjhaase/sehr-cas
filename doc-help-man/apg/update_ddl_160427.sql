CREATE GENERATOR GEN_PRSID;
SET GENERATOR GEN_PRSID TO 100;

CREATE DOMAIN DTNOW
 AS Timestamp
 DEFAULT 'NOW';

/* define '-1' as a Integer based domain */
CREATE DOMAIN "INTEG-1"
 AS Integer
 DEFAULT -1
 NOT NULL;
 
/* ip type correction char to varchar */
ALTER DOMAIN STRINGIP
  SET DEFAULT '0.0.0.0'
  -- | DROP DEFAULT
  -- | ADD [CONSTRAINT] CHECK (condition)
  -- | DROP CONSTRAINT
  -- | new_name
  TYPE varchar(15);

/* 16/05/05 changes related to self management */

-- responsible user that can edit the pubkey of the center, 
-- edit the administration data, open/close the binding etc.
-- on the zone host etc.; 
-- -1=there is no user that is responsible, 
-- 0=the module owner (using login by config settings) or
-- >0 a user recorded in USR_MAIN with ACL to edit 
-- 'his' center settings (NET_CENTER record) - nothing else 
ALTER TABLE NET_CENTER ADD USRID "INTEG-1";

/* implementation of relationships */


/* implementation of PKI */
ALTER TABLE NET_ZONES ADD PUBKEY BLOBOBJ;
ALTER TABLE NET_CENTER ADD PUBKEY BLOBOBJ;
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'A public key to requesting parties to let them verify the signature or to crypt messages and data for the center.'  where RDB$FIELD_NAME = 'PUBKEY' and RDB$RELATION_NAME = 'NET_CENTER';

/* the meaning of -1 is: no user assigned */ 
ALTER TABLE LC_MAIN ALTER USRID TYPE "INTEG-1";

SET TERM ^ ;
ALTER PROCEDURE SET_DEFAULTSERVICES (
    SVID Integer )
RETURNS (
    USRID Integer,
    STAT Integer,
    SV Integer,
    INSERTS Integer )
AS
DECLARE VARIABLE USRNAME VARCHAR(255);
DECLARE VARIABLE USRPASS VARCHAR(255);
begin
  INSERTS = 0;
  if (:SVID is null) then exit;
  --/ *
  for select usrid, usrname, usrpass from USR_MAIN where status = 0
      into :USRID, :USRNAME, :USRPASS
  do begin
    STAT = 0;
    for select svid from usr_services where usrid=:USRID into :sv
      do begin
        if (sv = :SVID) then begin
        STAT = 1; -- found  
      end
    end
    if (STAT = 0) then begin
       insert into usr_services (recid,usrid,svid,login,passwd)
         values (gen_id(gen_usrsvid, 1),:USRID,:SVID,:USRNAME,:USRPASS);
      INSERTS = INSERTS + 1;
    end
    -- suspend; 
  end
  --* /
end^
SET TERM ; ^

GRANT EXECUTE
 ON PROCEDURE SET_DEFAULTSERVICES TO  SYSDBA;

DROP PROCEDURE SET_DEFAULTSERVICES;
DROP PROCEDURE LOGIN2SEHRZONE;
COMMIT;
ALTER TABLE USR_MAIN ALTER USRPASS TYPE VCHAR255;
 
SET TERM ^ ;
CREATE PROCEDURE LOGIN2SEHRZONE (
    USRNAME Varchar(32),
    USRPASS Varchar(255), -- AES! 
    HOMEZONE Integer,
    SESSIONID Varchar(128) )
RETURNS (
    STATUS Integer,
    USRID Integer,
    LASTLOGIN Timestamp )
AS
DECLARE VARIABLE DTS TIMESTAMP;
begin
  STATUS=-1;
  DTS=current_timestamp;

  -- login passed if value of STATUS in USR_MAIN is '0' 
  select USRID,STATUS
    from  USR_MAIN
    where USRNAME=:USRNAME 
      and USRPASS=:USRPASS 
      and HOMEZONE=:HOMEZONE
    into :USRID,:STATUS;

  if (:USRID is not null) then begin
    -- we got a usrid, first logout from / invalidate old sessions  
    update acc_usrlog
      set logoutdts=:DTS,status=1
      where usrid=:USRID;
    --get last login dts 
    select max(logindts) from acc_usrlog 
      where usrid=:USRID into :LASTLOGIN;
    
  end else
    USRID = -1;

  -- log every access to the SEHR zone (modid=0), login failures
   --    may be identified by selection for STATUS=-1 and/or USRID=-1
  
  insert into acc_usrlog
    (acclogid,usrid,logindts,status,svid,sessionid)
    values (null,:USRID,:DTS,:STATUS,0,:SESSIONID);

  -- return value STATUS must be '0' for success 
  suspend;
end^
SET TERM ; ^

GRANT EXECUTE
 ON PROCEDURE LOGIN2SEHRZONE TO  SYSDBA;
 
SET TERM ^ ;
CREATE PROCEDURE SET_DEFAULTSERVICES (
    SVID Integer )
RETURNS (
    USRID Integer,
    STAT Integer,
    SV Integer,
    INSERTS Integer )
AS
DECLARE VARIABLE USRNAME VARCHAR(255);
DECLARE VARIABLE USRPASS VARCHAR(255);
begin
  INSERTS = 0;
  if (:SVID is null) then exit;
  
  for select usrid, usrname, usrpass from USR_MAIN where status = 0
      into :USRID, :USRNAME, :USRPASS
  do begin
    STAT = 0;
    for select svid from usr_services where usrid=:USRID into :sv
      do begin
        if (sv = :SVID) then begin
        STAT = 1; -- * found * 
      end
    end
    if (STAT = 0) then begin
       insert into usr_services (recid,usrid,svid,login,passwd)
         values (gen_id(gen_usrsvid, 1),:USRID,:SVID,:USRNAME,:USRPASS);
      INSERTS = INSERTS + 1;
    end
    -- * suspend; * 
  end
  
end^
SET TERM ; ^

GRANT EXECUTE
 ON PROCEDURE SET_DEFAULTSERVICES TO  SYSDBA;
 
ALTER TABLE USR_MAIN ADD PKIDATA BLOBOBJ;
 
ALTER TABLE LC_CAD ADD CV_SER Varchar(128);
ALTER TABLE LC_CAD ADD CV_ID Varchar(32);
-- type of card 
-- 0=smartcard ISO 7816
-- 1=USB memory card
ALTER TABLE LC_CAD ADD CV_CT SHORT DEFAULT 1;
-- status changed
ALTER TABLE LC_CAD ADD CHANGED DTNOW;
ALTER TABLE LC_CAD ADD VALIDTO TIMESTAMP;

CREATE TABLE EHR_MAIN
(
    ehrid integer NOT NULL,
    createdt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ehruid varchar(36),
    prsid integer,
    lcid integer,
    
    CONSTRAINT PK_EHR_MAIN
        PRIMARY KEY (ehrid)
);
CREATE TABLE EHR_COMP
(
    compid integer NOT NULL,
    ehrid integer not null,
    prsid integer not null, --prs_main, patient
    uid varchar(36),
    createdt TIMESTAMP,
    changedt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    templateid integer,
    archtypeid integer,
    nodeid VARCHAR(256),
    
    CONSTRAINT PK_EHR_COMP
        PRIMARY KEY (compid)
);
CREATE TABLE EHR_CONT
(
    contid integer NOT NULL,
    compid integer not null,
    parentid integer,
    nodeid VARCHAR(256),
    path VARCHAR(256),
    archtypeid integer,
    rm_attr_name varchar(128),
    rm_type_name varchar(128),
     
    CONSTRAINT PK_EHR_CONT
        PRIMARY KEY (contid)
);
ALTER TABLE LC_CAD DROP CONSTRAINT PK_LC_CAD;

alter table LC_CAD
  add constraint PK_LC_CAD
  primary key (CARDID);
  
CREATE INDEX IDX_LC_CAD1 ON LC_CAD
  (LCID);

ALTER TABLE LC_CAD DROP CONSTRAINT PK_LC_CAD;

alter table LC_CAD
  add constraint PK_LC_CAD
  primary key (CARDID, LCID);
  
ALTER TABLE LC_CAD ALTER CARDSTATUS TO STS;
ALTER TABLE LC_MAIN ADD EHNDomain VCHAR64;
ALTER TABLE LC_MAIN ALTER HOSTID TYPE VCHAR128;
ALTER TABLE DEF_MODULE ADD "TYPE" SHORT

DROP TRIGGER DEF_MODULE_BI;
ALTER TABLE DEF_MODULE DROP CONSTRAINT PK_DEF_MODULE;

DROP PROCEDURE LIST_SERVICES;

DROP PROCEDURE LIST_USRID_SERVICES;
-- COMMIT;

ALTER TABLE DEF_MODULE ADD ISXNET SHORT;
ALTER TABLE DEF_MODULE ADD VENDOR_ADRID "INTEGER";

ALTER TABLE DEF_MODULE ALTER RECID TO MODID;

ALTER TABLE DEF_MODULE ALTER MODID POSITION 1;
ALTER TABLE DEF_MODULE ALTER NAME POSITION 2;
ALTER TABLE DEF_MODULE ALTER TITLE POSITION 3;
ALTER TABLE DEF_MODULE ALTER PIK POSITION 4;
ALTER TABLE DEF_MODULE ALTER MOD_VERSION POSITION 5;
ALTER TABLE DEF_MODULE ALTER MOD_RELEASE POSITION 6;
ALTER TABLE DEF_MODULE ALTER LASTUPDATE POSITION 7;
ALTER TABLE DEF_MODULE ALTER NOTES POSITION 8;
ALTER TABLE DEF_MODULE ALTER ISXNET POSITION 9;
ALTER TABLE DEF_MODULE ALTER ISSTANDARD POSITION 10;
ALTER TABLE DEF_MODULE ALTER CATID POSITION 11;
ALTER TABLE DEF_MODULE ALTER "TYPE" POSITION 12;
ALTER TABLE DEF_MODULE ALTER ICON_SMALL POSITION 13;
ALTER TABLE DEF_MODULE ALTER URL POSITION 14;
ALTER TABLE DEF_MODULE ALTER GUID POSITION 15;
ALTER TABLE DEF_MODULE ALTER VENDOR_ADRID POSITION 16;

ALTER TABLE DEF_MODULE ALTER "TYPE" TYPE "INTEGER";

UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = NULL
WHERE RDB$FIELD_NAME = 'TYPE' AND RDB$RELATION_NAME = 'DEF_MODULE';


SET TERM ^ ;
CREATE OR ALTER PROCEDURE LIST_SERVICES (
    ZONEID_PAR Integer,
    CENTERID_PAR Integer,
    TITLE_PAR Varchar(128) )
RETURNS (
    SVID Integer,
    ZONEID Integer,
    CENTERID Integer,
    TITLE Varchar(128),
    CATID Integer,
    URLUSER Varchar(128),
    URLADMIN Varchar(128),
    STARTOFSERVICE Timestamp,
    ENDOFSERVICE Timestamp,
    ISPUBLIC Integer,
    ISSTANDARD Integer,
    CNT Integer )
AS
DECLARE VARIABLE MODID INTEGER;
BEGIN
  if (ZONEID_PAR > 0 ) then begin
    SELECT COUNT(*) FROM NET_SERVICES WHERE ZONEID=:ZONEID_PAR INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,M.ISPUBLIC,M.ISSTANDARD FROM NET_SERVICES N, DEF_MODULE M WHERE N.MODID=M.MODID AND N.ZONEID=:ZONEID_PAR
        INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:ISPUBLIC,:ISSTANDARD
    DO
      SUSPEND;
    EXIT;
  end
  else if (CENTERID_PAR > 0 ) then begin
    SELECT COUNT(*) FROM NET_SERVICES WHERE CENTERID=:CENTERID_PAR INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,M.ISPUBLIC,M.ISSTANDARD FROM NET_SERVICES N, DEF_MODULE M WHERE N.MODID=M.MODID AND N.CENTERID=:CENTERID_PAR
        INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:ISPUBLIC,:ISSTANDARD
    DO
      SUSPEND;
    EXIT;
  end
  else if ( TITLE_PAR is not null ) then begin
    SELECT COUNT(*) FROM NET_SERVICES WHERE TITLE CONTAINING :TITLE_PAR INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,M.ISPUBLIC,M.ISSTANDARD FROM NET_SERVICES N, DEF_MODULE M WHERE N.MODID=M.MODID AND N.TITLE CONTAINING :TITLE_PAR
        INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:ISPUBLIC,:ISSTANDARD
    DO
      SUSPEND;
    EXIT;
  end
  else begin
    SELECT COUNT(*) FROM NET_SERVICES INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,N.MODID FROM NET_SERVICES N ORDER BY N.ZONEID, N.CENTERID
        INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:MODID
    DO BEGIN
      ISPUBLIC=0;-- reset to default, they may not defined in def_module 
      ISSTANDARD=0;
      SELECT ISPUBLIC,ISSTANDARD FROM DEF_MODULE WHERE MODID=:MODID
        INTO :ISPUBLIC,:ISSTANDARD;
      SUSPEND;
    END
    EXIT;
  end

END^
SET TERM ; ^


GRANT EXECUTE
 ON PROCEDURE LIST_SERVICES TO  SYSDBA;

SET TERM ^ ;
CREATE OR ALTER PROCEDURE LIST_USRID_SERVICES (
    USRID Integer )
RETURNS (
    CATID Integer,
    CATTITLE Varchar(32),
    SVID Integer,
    SVTITLE Varchar(64),
    LOGIN Varchar(32),
    PASSWD Varchar(32),
    URLSTART Varchar(1024),
    URLDATA Varchar(1024),
    CENTERID Integer,
    INTRADOC Varchar(255),
    ZONEID Varchar(255),
    MENUTITLE Varchar(255),
    STATUS Smallint,
    ICONSMALL Varchar(32),
    MODNAME Varchar(32) )
AS
declare variable MODID integer;
BEGIN
  FOR SELECT SVID,MENUTITLE,LOGIN,PASSWD,URLSTART,URLDATA,STATUS FROM USR_SERVICES WHERE USRID=:USRID
    INTO :SVID,:MENUTITLE,:LOGIN,:PASSWD,:URLSTART,:URLDATA,:STATUS DO BEGIN
    if (:SVID > 0 ) then begin
      SELECT S.TITLE,S.CENTERID,S.CATID,C.CFGVALUE,S.ZONEID,S.MODID FROM NET_SERVICES S, DEF_OPTIONS C WHERE S.CATID=C.CFGKEY AND C.MASTERTYP='sehrmccat' AND S.SVID=:SVID
        INTO :SVTITLE,:CENTERID,:CATID,:CATTITLE,:ZONEID,:MODID;
      IF (CENTERID > 0) THEN SELECT INTRADOC FROM NET_CENTER WHERE CENTERID=:CENTERID INTO :INTRADOC;
      IF ((MENUTITLE IS NULL) or (MENUTITLE='')) THEN MENUTITLE = SVTITLE;
      IF (INTRADOC IS NULL) THEN INTRADOC = '';
      SELECT NAME, ICON_SMALL FROM DEF_MODULE WHERE MODID=:MODID INTO :MODNAME, :ICONSMALL;
      SUSPEND;
    end
  END
END^
SET TERM ; ^


GRANT EXECUTE
 ON PROCEDURE LIST_USRID_SERVICES TO  SYSDBA;
 
 SET TERM ^ ;

CREATE TRIGGER DEF_MODULE_BI FOR DEF_MODULE
ACTIVE BEFORE INSERT POSITION 0
AS 
BEGIN 
	IF(NEW.MODID IS NULL) THEN NEW.MODID=GEN_ID(GEN_MODID,1);
END^

SET TERM ; ^ 

UPDATE RDB$RELATION_FIELDS SET RDB$NULL_FLAG = NULL
WHERE RDB$FIELD_NAME = 'TYPE' AND RDB$RELATION_NAME = 'DEF_MODULE';

ALTER TABLE DEF_MODULE ALTER "TYPE" TO APPTYPE;

CREATE TABLE PRS_MAIN
(
    prsid INTEGER NOT NULL,
    firstname VARCHAR(64) DEFAULT NULL,
    lastname VARCHAR(64) DEFAULT NULL,
    dob DATE,
    gender SHORT DEFAULT 0,
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
SET TERM ^ ;
CREATE TRIGGER PRS_MAIN_BI FOR PRS_MAIN
ACTIVE BEFORE INSERT POSITION 0
AS 
BEGIN 
	IF (NEW.PRSID IS NULL) THEN NEW.PRSID = GEN_ID(GEN_PRSID,1);
END^
SET TERM ; ^

alter table USR_MAIN
add constraint FK_USR_PRS
foreign key (USRID) 
references PRS_MAIN (PRSID)
on update NO ACTION
on delete NO ACTION
;

CREATE TABLE PAT_MAIN
(
    patid INTEGER NOT NULL,
    info varchar(255) default '',
    --date of death
    dod Date DEFAULT null,
    socialid VCHAR(64),
    -- see DEF_OPTIONS.socialidtype for values
    socialidtype SHORT DEFAULT 0,
    CONSTRAINT PK_PAT_MAIN
      PRIMARY KEY (patid)
);

alter table PAT_MAIN
add constraint FK_PAT_PRS
foreign key (PATID) 
references PRS_MAIN (PRSID)
-- on update NO ACTION 
;

CREATE TABLE ADR_MAIN
(
  ADRID INTEG DEFAULT 0 NOT NULL,
  MATCH CHAR32 DEFAULT '',
  TITLE VCHAR32 DEFAULT '',
  ADR1 VCHAR32 DEFAULT '',
  ADR2 VCHAR32 DEFAULT '',
  ADR3 VCHAR32 DEFAULT '',
  STREET VCHAR32 DEFAULT '',
  COUNTRYCODE Varchar(3),
  ZIP VCHAR16 DEFAULT '',
  CITY VCHAR32 DEFAULT '',
  COUNTRY VCHAR32 DEFAULT '',
  FON1 VCHAR255 DEFAULT '',
  FAX1 VCHAR255 DEFAULT '',
  EMAIL1 VCHAR255 DEFAULT '',
  MOBILE1 VCHAR32 DEFAULT '',
  CHANGED Date DEFAULT 'NOW',
  INFO BLOBTXT,
  CONTACTITEM BLOBOBJ,
  ITEMID "INTEGER" DEFAULT 0,
  GEOLAT Decimal(11,7),
  GEOLNG Decimal(11,7),
  ADRTYPE SHORT DEFAULT 0,
  CONSTRAINT PK_ADR_MAIN PRIMARY KEY (ADRID)
);

UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'unique record id'  where RDB$FIELD_NAME = 'ADRID' and RDB$RELATION_NAME = 'ADR_MAIN';
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'Field SUBJECT of ContactItem'  where RDB$FIELD_NAME = 'MATCH' and RDB$RELATION_NAME = 'ADR_MAIN';
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'Field BODY of ContactItem'  where RDB$FIELD_NAME = 'INFO' and RDB$RELATION_NAME = 'ADR_MAIN';
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'transferable ContactItem object from which is record part of'  where RDB$FIELD_NAME = 'CONTACTITEM' and RDB$RELATION_NAME = 'ADR_MAIN';
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'The id of the contact item; each contact item has an own id which is not the record id; if a ContactItem has been transferred from one SEHR host to another the rec id is not identical - so we use an own unique item id!'  where RDB$FIELD_NAME = 'ITEMID' and RDB$RELATION_NAME = 'ADR_MAIN';
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'Type of address; -1=n/a, 0=Internal/SEHR CAS Location, 1=A Person (USR_MAIN), 2=Module Producer (DEF_MODULE FK)'  where RDB$FIELD_NAME = 'ADRTYPE' and RDB$RELATION_NAME = 'ADR_MAIN';
GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE
 ON ADR_MAIN TO  SYSDBA WITH GRANT OPTION;


-- ALTER TABLE ADR_MAIN ADD GEOLAT Decimal(10,6);
-- ALTER TABLE ADR_MAIN ADD GEOLNG Decimal(10,6);
-- ALTER TABLE ADR_MAIN ADD ADRTYPE SHORT;
-- ALTER TABLE ADR_MAIN ALTER GEOLAT TYPE Decimal(11,7);
-- ALTER TABLE ADR_MAIN ALTER GEOLNG TYPE Decimal(11,7);

ALTER TABLE DEF_MODULE ALTER VENDOR_ADRID TO PRODUCER_ADRID;


SET TERM ^ ;
CREATE TRIGGER ADR_MAIN_BI FOR ADR_MAIN
ACTIVE BEFORE INSERT POSITION 0
AS 
BEGIN 
	 IF(NEW.ADRID IS NULL) THEN NEW.ADRID=GEN_ID(GEN_ADRID,1);
END^
SET TERM ; ^

ALTER TABLE LC_CAD ALTER LCID POSITION 1;
ALTER TABLE LC_CAD ALTER CARDID POSITION 2;
ALTER TABLE LC_CAD ALTER CII_MII POSITION 3;
ALTER TABLE LC_CAD ALTER CII_CC POSITION 4;
ALTER TABLE LC_CAD ALTER CII_II POSITION 5;
ALTER TABLE LC_CAD ALTER CII_CHECKDIGIT POSITION 6;
ALTER TABLE LC_CAD ALTER CI POSITION 7;
ALTER TABLE LC_CAD ALTER STS POSITION 8;
ALTER TABLE LC_CAD ALTER CAI POSITION 9;
ALTER TABLE LC_CAD ALTER CAI_CAT POSITION 10;
ALTER TABLE LC_CAD ALTER CAI_CAV POSITION 11;
ALTER TABLE LC_CAD ALTER CV_SER POSITION 12;

ALTER TABLE NET_SERVICES ADD URLTXDATA VCHAR255;
ALTER TABLE NET_SERVICES ADD SEHRAUTHKEY VCHAR128;

ALTER TABLE LC_MAIN ADD COUNTRY Char(2);

-- external ID if not a numer, e.g. a GUID
ALTER TABLE LC_MAIN ADD PATEXTID1 VCHAR255;
-- reference to external ID, e.g. PACS PATIENT
ALTER TABLE LC_MAIN ADD EXTID1DESC VCHAR255;
ALTER TABLE LC_MAIN ADD PATEXTID2 VCHAR255;
ALTER TABLE LC_MAIN ADD EXTID2DESC VCHAR255;
-- local reference to a registered person
-- only PRSID is referenced to EHR_MAIN 
-- (case file on the zone host)
-- USRID is referenced to USR_MAIN to access / login to
-- services
ALTER TABLE LC_MAIN ADD PRSID INTEGER;

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

UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'see table def_options; ''personel_relation'', ''ehr_follower'',
''ehr_producer'', ''note_follower'',
''note_producer'', ''ignore_bounce'',
''suggested_for'' ...
'  where RDB$FIELD_NAME = 'RELTYPE' and RDB$RELATION_NAME = 'PRS_RELSHIP';
UPDATE RDB$RELATIONS set
RDB$DESCRIPTION = 'see sehr-cas/doc-help-man/specification/person_relationships.pdf
'
where RDB$RELATION_NAME = 'PRS_RELSHIP';
GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE
 ON PRS_RELSHIP TO  SYSDBA WITH GRANT OPTION;


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

SET TERM ^ ;
ALTER TRIGGER USR_MAIN_BI ACTIVE
BEFORE INSERT POSITION 0
AS
BEGIN
  IF(NEW.USRID IS NULL) THEN NEW.USRID=GEN_ID(GEN_PRSID,1);
END^
SET TERM ; ^

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


GRANT EXECUTE
 ON PROCEDURE LIST_USERS TO  SYSDBA;

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

UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'unique record id'  
  where RDB$FIELD_NAME = 'ADRID' and RDB$RELATION_NAME = 'PRS_CONTACT';
UPDATE RDB$RELATION_FIELDS set RDB$DESCRIPTION = 'see DEF_OPTIONS, adresscat, for options'  
  where RDB$FIELD_NAME = 'OPTID' and RDB$RELATION_NAME = 'PRS_CONTACT';
GRANT DELETE, INSERT, REFERENCES, SELECT, UPDATE
 ON PRS_CONTACT TO  SYSDBA WITH GRANT OPTION;

-- usrid id deprecated, lc is assigned to a person, 
-- a patient object is based on a person
update lc_main set prsid = usrid;



