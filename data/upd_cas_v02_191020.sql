ALTER TABLE DEF_MODULE DROP CONSTRAINT PK_DEF_MODULE;
commit;

DROP TRIGGER DEF_MODULE_BI;
DROP PROCEDURE LIST_SERVICES;
DROP PROCEDURE LIST_USRID_SERVICES;
commit;

/*
ALTER TABLE DEF_MODULE ALTER RECID TO MODID;

alter table DEF_MODULE
  add constraint PK_DEF_MODULE
  primary key (MODID);

ALTER TABLE DEF_MODULE ADD 
PRODUCER_ADRID INTEG NOT NULL;

COMMIT;
UPDATE DEF_MODULE 
SET PRODUCER_ADRID = '0' 
WHERE PRODUCER_ADRID IS NULL;
ALTER TABLE DEF_MODULE ADD 
ISXNET "BOOLEAN" NOT NULL;

COMMIT;
UPDATE DEF_MODULE 
SET ISXNET = '0' 
WHERE ISXNET IS NULL;
ALTER TABLE DEF_MODULE ADD 
APPTYPE INTEG NOT NULL;

COMMIT;
UPDATE DEF_MODULE 
SET APPTYPE = '0' 
WHERE APPTYPE IS NULL;
commit;

*/

/*
SET TERM ^ ;

CREATE TRIGGER DEF_MODULE_BI FOR DEF_MODULE
ACTIVE BEFORE INSERT POSITION 0
AS 
BEGIN 
  IF(NEW.MODID IS NULL) THEN NEW.MODID=GEN_ID(GEN_MODID,1);
END^

SET TERM ; ^ 
SET TERM ^ ;
CREATE PROCEDURE LIST_USRID_SERVICES (
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
  FOR SELECT SVID,MENUTITLE,LOGIN,PASSWD,URLSTART,URLDATA,STATUS 
      FROM USR_SERVICES WHERE USRID=:USRID
        INTO :SVID,:MENUTITLE,:LOGIN,:PASSWD,:URLSTART,:URLDATA,:STATUS 
    DO BEGIN
    if (:SVID > 0 ) then begin
      SELECT S.TITLE,S.CENTERID,S.CATID,C.CFGVALUE,S.ZONEID,S.MODID 
        FROM NET_SERVICES S, DEF_OPTIONS C 
          WHERE S.CATID=C.CFGKEY AND C.MASTERTYP='sehrmccat' AND S.SVID=:SVID
          INTO :SVTITLE,:CENTERID,:CATID,:CATTITLE,:ZONEID,:MODID;
      --IF (CENTERID > 0) THEN SELECT INTRADOC FROM NET_CENTER WHERE CENTERID=:CENTERID INTO :INTRADOC;
      IF ((MENUTITLE IS NULL) or (MENUTITLE='')) THEN MENUTITLE = SVTITLE;
      --IF (INTRADOC IS NULL) THEN INTRADOC = '';
      SELECT NAME, ICON_SMALL FROM DEF_MODULE WHERE MODID=:MODID INTO :MODNAME, :ICONSMALL;
      SUSPEND;
    end
  END
END^
SET TERM ; ^

GRANT EXECUTE
 ON PROCEDURE LIST_USRID_SERVICES TO  SYSDBA;


SET TERM ^ ;
CREATE PROCEDURE LIST_SERVICES (
    ZONEID_PAR Integer,
    CENTERID_PAR Integer,
    PIK_PAR Varchar(128) )
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
    ISXNET Smallint,
    ISSTANDARD Smallint,
    CNT Integer )
AS
DECLARE VARIABLE MODID INTEGER;
BEGIN
  if (ZONEID_PAR > 0 ) then begin
    SELECT COUNT(*) FROM NET_SERVICES WHERE ZONEID=:ZONEID_PAR INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,M.ISXNET,M.ISSTANDARD 
        FROM NET_SERVICES N, DEF_MODULE M WHERE N.MODID=M.MODID AND N.ZONEID=:ZONEID_PAR
          INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:ISXNET,:ISSTANDARD
    DO
      SUSPEND;
    EXIT;
  end
  else if (CENTERID_PAR > 0 ) then begin
    SELECT COUNT(*) FROM NET_SERVICES WHERE CENTERID=:CENTERID_PAR INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,M.ISXNET,M.ISSTANDARD FROM NET_SERVICES N, DEF_MODULE M WHERE N.MODID=M.MODID AND N.CENTERID=:CENTERID_PAR
        INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:ISXNET,:ISSTANDARD
    DO
      SUSPEND;
    EXIT;
  end
  else if ( PIK_PAR is not null ) then begin
    SELECT COUNT(N.SVID) FROM NET_SERVICES N WHERE N.MODID IN (SELECT M.MODID FROM DEF_MODULE M WHERE M.PIK=:PIK_PAR) INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,M.ISXNET,M.ISSTANDARD 
        FROM NET_SERVICES N, DEF_MODULE M WHERE N.MODID=M.MODID AND M.PIK=:PIK_PAR
          INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:ISXNET,:ISSTANDARD
    DO
      SUSPEND;
    EXIT;
  end
  else begin
    SELECT COUNT(*) FROM NET_SERVICES INTO :CNT;
    FOR
      SELECT N.SVID,N.CATID,N.ZONEID,N.CENTERID,N.TITLE,N.URLUSER,N.URLADMIN,N.STARTOFSERVICE,N.ENDOFSERVICE,N.MODID 
        FROM NET_SERVICES N ORDER BY N.ZONEID, N.CENTERID
          INTO :SVID, :CATID, :ZONEID,:CENTERID,:TITLE,:URLUSER,:URLADMIN,:STARTOFSERVICE,:ENDOFSERVICE,:MODID
    DO BEGIN
      ISXNET=0; 
      ISSTANDARD=0;
      SELECT ISXNET,ISSTANDARD FROM DEF_MODULE WHERE MODID=:MODID
        INTO :ISXNET,:ISSTANDARD;
      SUSPEND;
    END
    EXIT;
  end

END^
SET TERM ; ^

GRANT EXECUTE
 ON PROCEDURE LIST_SERVICES TO  SYSDBA;

*/
