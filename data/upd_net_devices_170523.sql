CREATE TABLE NET_DEVICE
(
    deviceid VCHAR255 not null, 
    name VCHAR32,
    prsid_op INTEG, --operated, used by
    prsid_resp INTEG, -- responsible person (adr by prs_contact)
    
    CONSTRAINT PK_DEVICEID PRIMARY KEY (deviceid)
);
