/* AccessURL */
if exists (select name from dbo.sysobjects where name = 'AccessURL' and type = 'U')
drop table dbo.AccessURL
GO

create table dbo.AccessURL
(
   AccessURL nvarchar(200) not null,
   PortalClientID integer not null,
   constraint pk_accessurl primary key nonclustered (AccessURL)
)
GO

alter table AccessURL set (lock_escalation = auto)
GO

/* ApiAuthorization */
if exists (select name from dbo.sysobjects where name = 'ApiAuthorization' and type = 'U')
drop table dbo.ApiAuthorization
GO

create table dbo.ApiAuthorization
(
   ID integer identity(1,1) not null,
   PortalUserID integer,
   ApiSecret nvarchar(200),
   RefreshToken nvarchar(max),
   constraint pk_apiauthorization primary key nonclustered (ApiSecret)
)
GO

alter table ApiAuthorization set (lock_escalation = auto)
GO

create index ix_apiauthorization1 on dbo.ApiAuthorization (PortalUserID)
GO

/* Audit */
if exists (select name from dbo.sysobjects where name = 'Audit' and type = 'U')
drop table dbo.Audit
GO

create table dbo.Audit
(
   ID bigint identity(1,1) not null,
   ChangedBy nvarchar(254) not null,
   ChangedOn datetime not null,
   RecordID nvarchar(255),
   RecordName nvarchar(500) not null,
   Category nvarchar(100) not null,
   Item nvarchar(100) not null,
   Change nchar(1) not null,
   OldValue nvarchar(max),
   NewValue nvarchar(max),
   PortalClientID integer not null,
   constraint pk_audit primary key nonclustered (ID),
   constraint ck_audit_change_95E67894 check(Change in ('A','D','M'))
)
GO

alter table Audit set (lock_escalation = auto)
GO

create index ix_audit1 on dbo.Audit (ChangedOn,PortalClientID)
GO

/* AutomationAuthorization */
if exists (select name from dbo.sysobjects where name = 'AutomationAuthorization' and type = 'U')
drop table dbo.AutomationAuthorization
GO

create table dbo.AutomationAuthorization
(
   AutomationSecret uniqueidentifier not null constraint df_automatio_automatio_8D75AC64 default newid(),
   RefreshToken nvarchar(max) not null,
   ExpirationDateTime datetime not null,
   CreatedOnDateTime datetime not null,
   LastUsedDateTime datetime,
   PortalUserID integer not null,
   constraint pk_automationauthorization primary key nonclustered (AutomationSecret)
)
GO

alter table AutomationAuthorization set (lock_escalation = auto)
GO

create index ix_automationauthorization1 on dbo.AutomationAuthorization (PortalUserID)
GO

/* Brand */
if exists (select name from dbo.sysobjects where name = 'Brand' and type = 'U')
drop table dbo.Brand
GO

create table dbo.Brand
(
   ID integer identity(1,1) not null,
   Name nvarchar(max) not null,
   constraint pk_brand primary key nonclustered (ID),
   constraint ak_brand1 unique (Name)
)
GO

alter table Brand set (lock_escalation = auto)
GO

insert into Brand (Name) values ('Hilton');
            insert into Brand (Name) values ('Marriott International');
            insert into Brand (Name) values ('Hyatt Hotels Corporation');
            insert into Brand (Name) values ('IHG Hotels & Resorts');
            insert into Brand (Name) values ('Choice Hotels');
            insert into Brand (Name) values ('Wyndham Hotels & Resorts');
            insert into Brand (Name) values ('Other Notable Brands & Independent Collections');
GO

/* Certificate */
if exists (select name from dbo.sysobjects where name = 'Certificate' and type = 'U')
drop table dbo.Certificate
GO

create table dbo.Certificate
(
   Alias nvarchar(max) not null,
   Certificate nvarchar(max),
   PrivateKey nvarchar(max),
   constraint pk_certificate primary key nonclustered (Alias)
)
GO

alter table Certificate set (lock_escalation = auto)
GO

/* ClientPreference */
if exists (select name from dbo.sysobjects where name = 'ClientPreference' and type = 'U')
drop table dbo.ClientPreference
GO

create table dbo.ClientPreference
(
   ID bigint identity(1,1) not null,
   PortalClientID integer not null,
   PrefKey nvarchar(200) not null,
   PrefValue nvarchar(max) not null,
   constraint pk_clientpreference primary key nonclustered (ID),
   constraint ak_clientpreference1 unique (PortalClientID,PrefKey)
)
GO

alter table ClientPreference set (lock_escalation = auto)
GO

/* ClientProduct */
if exists (select name from dbo.sysobjects where name = 'ClientProduct' and type = 'U')
drop table dbo.ClientProduct
GO

create table dbo.ClientProduct
(
   ClientID integer not null,
   ProductID integer not null,
   constraint pk_clientproduct primary key nonclustered (ClientID,ProductID)
)
GO

alter table ClientProduct set (lock_escalation = auto)
GO

/* ClientRegion */
if exists (select name from dbo.sysobjects where name = 'ClientRegion' and type = 'U')
drop table dbo.ClientRegion
GO

create table dbo.ClientRegion
(
   ID integer identity(1,1) not null,
   PortalClientID integer not null,
   Name nvarchar(max) not null,
   ShortName nvarchar(25) not null,
   Code nvarchar(25) not null,
   ExcludeFromCorpReports nchar(1) not null constraint df_clientreg_excludefr_857B8591 default 'N',
   constraint pk_clientregion primary key nonclustered (ID),
   constraint ak_clientregion1 unique (PortalClientID,Name),
   constraint ak_clientregion2 unique (PortalClientID,Code),
   constraint ck_clientreg_excludefr_857B8591 check(ExcludeFromCorpReports in ('Y','N'))
)
GO

alter table ClientRegion set (lock_escalation = auto)
GO

/* CredentialsHistory */
if exists (select name from dbo.sysobjects where name = 'CredentialsHistory' and type = 'U')
drop table dbo.CredentialsHistory
GO

create table dbo.CredentialsHistory
(
   Credentials nvarchar(500) not null,
   CredentialsDateTime datetime not null,
   constraint pk_credentialshistory primary key nonclustered (Credentials)
)
GO

alter table CredentialsHistory set (lock_escalation = auto)
GO

create index ix_credentialshistory1 on dbo.CredentialsHistory (CredentialsDateTime)
GO

/* ExpiredCredentials */
if exists (select name from dbo.sysobjects where name = 'ExpiredCredentials' and type = 'U')
drop table dbo.ExpiredCredentials
GO

create table dbo.ExpiredCredentials
(
   Credentials nvarchar(500) not null,
   ExpiredOnDateTime datetime not null,
   constraint pk_expiredcredentials primary key nonclustered (Credentials)
)
GO

alter table ExpiredCredentials set (lock_escalation = auto)
GO

/* GlobalData */
if exists (select name from dbo.sysobjects where name = 'GlobalData' and type = 'U')
drop table dbo.GlobalData
GO

create table dbo.GlobalData
(
   ID integer identity(1,1) not null,
   Tag nvarchar(50) not null,
   TagValue nvarchar(max),
   TagData varbinary(max),
   constraint pk_globaldata primary key nonclustered (ID),
   constraint ak_globaldata1 unique (Tag)
)
GO

alter table GlobalData set (lock_escalation = auto)
GO

/* ImpersonateHistory */
if exists (select name from dbo.sysobjects where name = 'ImpersonateHistory' and type = 'U')
drop table dbo.ImpersonateHistory
GO

create table dbo.ImpersonateHistory
(
   PortalUserIDToImpersonate integer not null,
   PortalUserIDOfImpersonator integer not null,
   LoginDateTime datetime not null,
   constraint pk_impersonatehistory primary key nonclustered (PortalUserIDToImpersonate,PortalUserIDOfImpersonator,LoginDateTime)
)
GO

alter table ImpersonateHistory set (lock_escalation = auto)
GO

/* LDAPConfig */
if exists (select name from dbo.sysobjects where name = 'LDAPConfig' and type = 'U')
drop table dbo.LDAPConfig
GO

create table dbo.LDAPConfig
(
   ID integer identity(1,1) not null,
   URL nvarchar(200) not null,
   BaseDN nvarchar(200) not null,
   SearchFilter nvarchar(200) not null,
   Domain nvarchar(200),
   UserNamePattern nvarchar(200),
   constraint pk_ldapconfig primary key nonclustered (ID)
)
GO

alter table LDAPConfig set (lock_escalation = auto)
GO

/* LoginHistory */
if exists (select name from dbo.sysobjects where name = 'LoginHistory' and type = 'U')
drop table dbo.LoginHistory
GO

create table dbo.LoginHistory
(
   ID integer identity(1,1) not null,
   PortalUserID integer,
   LoginDateTime datetime not null,
   LoginStatus nvarchar(50) not null,
   DatabaseID integer,
   PropertyID integer,
   PropertyName nvarchar(50),
   LoginID nvarchar(254),
   FailureReason nvarchar(max),
   constraint pk_loginhistory primary key nonclustered (PortalUserID,LoginDateTime,LoginStatus)
)
GO

alter table LoginHistory set (lock_escalation = auto)
GO

/* NewUserRegistration */
if exists (select name from dbo.sysobjects where name = 'NewUserRegistration' and type = 'U')
drop table dbo.NewUserRegistration
GO

create table dbo.NewUserRegistration
(
   ID uniqueidentifier not null constraint df_newuserre_id_EBD2F996 default newid(),
   Type nvarchar(8) not null,
   PartnerCode nvarchar(max) not null,
   LoginID nvarchar(max) not null,
   VerificationCode nvarchar(max) not null,
   ClientID integer not null,
   LoginClientID integer not null,
   CreatedOn datetime not null,
   CreatedByPortalUserID integer not null,
   UpdatedByPortalUserID integer,
   ExpiresOn datetime not null,
   NumberVerifyFailures integer not null,
   NumberOfAttempts integer,
   StartedOn datetime,
   CompletedOn datetime,
   PortalUserID integer,
   DatabaseID integer,
   PropertyID integer,
   EmployeeID integer,
   Email nvarchar(max),
   MobilePhone nvarchar(max),
   FirstName nvarchar(max),
   MiddleName nvarchar(max),
   LastName nvarchar(max),
   Locale nvarchar(max),
   constraint pk_newuserregistration primary key nonclustered (ID),
   constraint ak_newuserregistration1 unique (PartnerCode,LoginID,VerificationCode),
   constraint ck_newuserre_type_430048F5 check(Type in ('EMPLOYEE','USER'))
)
GO

alter table NewUserRegistration set (lock_escalation = auto)
GO

/* PasswordBlacklist */
if exists (select name from dbo.sysobjects where name = 'PasswordBlacklist' and type = 'U')
drop table dbo.PasswordBlacklist
GO

create table dbo.PasswordBlacklist
(
   ID integer identity(1,1) not null,
   PasswordPattern nvarchar(255) not null,
   constraint pk_passwordblacklist primary key nonclustered (ID),
   constraint ak_passwordblacklist1 unique (PasswordPattern)
)
GO

alter table PasswordBlacklist set (lock_escalation = auto)
GO

/* PasswordQuestion */
if exists (select name from dbo.sysobjects where name = 'PasswordQuestion' and type = 'U')
drop table dbo.PasswordQuestion
GO

create table dbo.PasswordQuestion
(
   ID integer identity(1,1) not null,
   Locale nvarchar(10) not null,
   Question nvarchar(255) not null,
   constraint pk_passwordquestion primary key nonclustered (ID),
   constraint ak_passwordquestion1 unique (Locale,Question)
)
GO

alter table PasswordQuestion set (lock_escalation = auto)
GO

/* PasswordRules */
if exists (select name from dbo.sysobjects where name = 'PasswordRules' and type = 'U')
drop table dbo.PasswordRules
GO

create table dbo.PasswordRules
(
   ID integer identity(1,1) not null,
   Pattern nvarchar(100) not null,
   Description nvarchar(200) not null,
   Duration smallint,
   ExpirationWarning smallint,
   NumberPrior smallint,
   MaxAttempts smallint,
   ResetsPerDay integer not null constraint df_passwordr_resetsper_E8192F86 default 3,
   constraint pk_passwordrules primary key nonclustered (ID),
   constraint ck_passwordr_duration_D74EEB37 check(Duration >= 1),
   constraint ck_passwordr_expiratio_EF8FDBCA check(ExpirationWarning >= 1),
   constraint ck_passwordr_numberpri_F247D89E check(NumberPrior >= 0),
   constraint ck_passwordr_resetsper_E8192F86 check(ResetsPerDay >= 1 and ResetsPerDay <= 3)
)
GO

alter table PasswordRules set (lock_escalation = auto)
GO

/* PortalAdminUserAdditionalClients */
if exists (select name from dbo.sysobjects where name = 'PortalAdminUserAdditionalClients' and type = 'U')
drop table dbo.PortalAdminUserAdditionalClients
GO

create table dbo.PortalAdminUserAdditionalClients
(
   ID integer identity(1,1) not null,
   UserID integer not null,
   ClientID integer not null,
   constraint pk_portaladminuseradditionalclie primary key nonclustered (ID),
   constraint ak_portaladminuseradditionalcli1 unique (UserID,ClientID)
)
GO

alter table PortalAdminUserAdditionalClients set (lock_escalation = auto)
GO

/* PortalClient */
if exists (select name from dbo.sysobjects where name = 'PortalClient' and type = 'U')
drop table dbo.PortalClient
GO

create table dbo.PortalClient
(
   ID integer identity(1,1) not null,
   GlobalID uniqueidentifier not null constraint df_portalcli_globalid_CADF1806 default newid(),
   Name nvarchar(50) not null,
   Code nvarchar(25) not null,
   Active nchar(1) not null constraint df_portalcli_active_AFB1450E default 'Y',
   ParentClientID integer,
   DatabaseID integer,
   KioskURL nvarchar(255),
   License varbinary(max),
   ValidPattern nvarchar(100),
   ValidDescription nvarchar(200),
   Duration smallint,
   NumberPrior smallint,
   MaxAttempts smallint,
   AccessURL nvarchar(200),
   RedirectURL nvarchar(200),
   LDAPID integer,
   SAMLID integer,
   PasswordRulesID integer,
   ImpersonationEnabled nchar(1) not null constraint df_portalcli_impersona_D4186AAD default 'N',
   Type nvarchar(9) not null constraint df_portalcli_type_9023742 default 'UNIPORTAL',
   ExpirationWarning smallint,
   InviteLoginClientID integer,
   ManagerInviteLoginClientID integer,
   AutomationApiSecret nvarchar(max),
   constraint pk_portalclient primary key nonclustered (ID),
   constraint ak_portalclient1 unique (GlobalID),
   constraint ak_portalclient2 unique (Name),
   constraint ck_portalcli_active_AFB1450E check(Active in ('Y','N')),
   constraint ck_portalcli_impersona_D4186AAD check(ImpersonationEnabled in ('Y','N')),
   constraint ck_portalcli_type_9023742 check(Type in ('LDAP','SAML','UNIPORTAL')),
   constraint ck_portalcli_expiratio_F18A7705 check(ExpirationWarning >= 1)
)
GO

alter table PortalClient set (lock_escalation = auto)
GO

/* PortalProperty */
if exists (select name from dbo.sysobjects where name = 'PortalProperty' and type = 'U')
drop table dbo.PortalProperty
GO

create table dbo.PortalProperty
(
   ID uniqueidentifier not null constraint df_portalpro_id_644783D9 default newid(),
   PortalClientID integer not null,
   Name nvarchar(max) not null,
   Code nvarchar(max) not null,
   Active nchar(1) not null constraint df_portalpro_active_1D4166C4 default 'Y',
   License nvarchar(max) not null,
   TimeZoneID nvarchar(max) not null,
   DefaultLocale nvarchar(max) not null constraint df_portalpro_defaultlo_F5D1F65D default 'en-US',
   TapsDatabaseID integer,
   PropertyType nvarchar(21),
   MarketType nvarchar(10),
   BrandID integer,
   NumberRooms integer,
   AddressLine1 nvarchar(max) not null,
   AddressLine2 nvarchar(max),
   Locality nvarchar(max) not null,
   AdministrativeArea nvarchar(max) not null,
   PostalCode nvarchar(max) not null,
   Country nvarchar(max) not null,
   ClientRegionID integer,
   Unionized nchar(1) constraint df_portalpro_unionized_4DC70061 default 'N',
   OpsDateFormat nvarchar(max) not null constraint df_portalpro_opsdatefo_7C8BEBD9 default 'MM/dd/yyyy',
   LogoReference nvarchar(max),
   constraint pk_portalproperty primary key nonclustered (ID),
   constraint ak_portalproperty1 unique (PortalClientID,Name),
   constraint ak_portalproperty2 unique (PortalClientID,Code),
   constraint ck_portalpro_active_1D4166C4 check(Active in ('Y','N')),
   constraint ck_portalpro_propertyt_B0301BCD check(PropertyType in ('FULL_SERVICE_HOTEL','SELECT_SERVICE_HOTEL','EXTENDED_STAY_HOTEL','RESORT','BOUTIQUE_HOTEL','CONVENTION_HOTEL','CASINO_HOTEL','MIXED_USE','LIMITED_SERVICE_HOTEL','ALL_INCLUSIVE_RESORT','TIMESHARE','GOLF_COURSE','RESTAURANT','OTHER')),
   constraint ck_portalpro_markettyp_5E27E34 check(MarketType in ('URBAN','SUBURBAN','AIRPORT','RESORT','SMALL_TOWN','HIGHWAY','CONVENTION','CAMPUS','OTHER')),
   constraint ck_portalpro_unionized_4DC70061 check(Unionized in ('Y','N'))
)
GO

alter table PortalProperty set (lock_escalation = auto)
GO

/* PortalPropertyAdditionalLocale */
if exists (select name from dbo.sysobjects where name = 'PortalPropertyAdditionalLocale' and type = 'U')
drop table dbo.PortalPropertyAdditionalLocale
GO

create table dbo.PortalPropertyAdditionalLocale
(
   ID uniqueidentifier not null constraint df_portalpro_id_D28BC4F8 default newid(),
   PortalPropertyID uniqueidentifier not null,
   Locale nvarchar(max) not null,
   constraint pk_portalpropertyadditionallocal primary key nonclustered (ID),
   constraint ak_portalpropertyadditionalloca1 unique (PortalPropertyID,Locale)
)
GO

alter table PortalPropertyAdditionalLocale set (lock_escalation = auto)
GO

/* PortalUser */
if exists (select name from dbo.sysobjects where name = 'PortalUser' and type = 'U')
drop table dbo.PortalUser
GO

create table dbo.PortalUser
(
   ID integer identity(1,1) not null,
   LoginClientID integer,
   LoginId nvarchar(254) not null,
   GlobalID uniqueidentifier not null constraint df_portaluse_globalid_8FF79506 default newid(),
   PWord nvarchar(200) not null,
   Active nchar(1) constraint df_portaluse_active_EAC5020E default 'N',
   ActiveChangedOn datetime,
   FirstName nvarchar(50),
   MiddleName nvarchar(50),
   LastName nvarchar(50),
   EMail nvarchar(254),
   MobilePhone nvarchar(100),
   QuestionID integer,
   Answer nvarchar(100),
   LastLogin datetime,
   PwordChanged datetime,
   PwordChangedSameDayCount integer,
   Locale nvarchar(10),
   ClientID integer,
   DefaultDatabaseID integer,
   BypassLdap nchar(1) not null constraint df_portaluse_bypasslda_84871397 default 'N',
   LdapUserName nvarchar(254),
   Flags integer,
   Photo varbinary(max),
   CreatedOn datetime,
   CreatedByPortalUserID integer,
   CanAdministerUsers nchar(1) not null constraint df_portaluse_canadmini_7918778 default 'N',
   constraint pk_portaluser primary key nonclustered (ID),
   constraint ak_portaluser1 unique (LoginClientID,LoginId),
   constraint ak_portaluser2 unique (GlobalID),
   constraint ck_portaluse_active_EAC5020E check(Active in ('Y','N')),
   constraint ck_portaluse_bypasslda_84871397 check(BypassLdap in ('Y','N')),
   constraint ck_portaluse_canadmini_7918778 check(CanAdministerUsers in ('Y','N'))
)
GO

alter table PortalUser set (lock_escalation = auto)
GO

create index ix_portaluser1 on dbo.PortalUser (EMail)
GO

/* PortalUserAudit */
if exists (select name from dbo.sysobjects where name = 'PortalUserAudit' and type = 'U')
drop table dbo.PortalUserAudit
GO

create table dbo.PortalUserAudit
(
   ID bigint identity(1,1) not null,
   PortalUserID integer not null,
   LoginClientID integer,
   LoginId nvarchar(254) not null,
   PWord nvarchar(200) not null,
   Active nchar(1) constraint df_portaluse_active_855DA7A1 default 'N',
   FirstName nvarchar(50),
   MiddleName nvarchar(50),
   LastName nvarchar(50),
   EMail nvarchar(254),
   MobilePhone nvarchar(100),
   QuestionID integer,
   Answer nvarchar(100),
   LastLogin datetime,
   PwordChanged datetime,
   Locale nvarchar(10),
   ClientID integer,
   DefaultDatabaseID integer,
   BypassLdap nchar(1) not null constraint df_portaluse_bypasslda_305CB7AA default 'N',
   LdapUserName nvarchar(254),
   Flags integer,
   ChangedOn datetime not null,
   Operation char(1) not null,
   Command nvarchar(max) not null,
   HostName nvarchar(128) not null,
   ChangedBySqlUser nvarchar(128) not null,
   ChangedByPortalUserID integer,
   CanAdministerUsers nchar(1) constraint df_portaluse_canadmini_E697688B default 'N',
   constraint pk_portaluseraudit primary key nonclustered (ID),
   constraint ck_portaluse_active_855DA7A1 check(Active in ('Y','N')),
   constraint ck_portaluse_bypasslda_305CB7AA check(BypassLdap in ('Y','N')),
   constraint ck_portaluse_canadmini_E697688B check(CanAdministerUsers in ('Y','N'))
)
GO

alter table PortalUserAudit set (lock_escalation = auto)
GO

create index ix_portaluseraudit1 on dbo.PortalUserAudit (LoginId,ChangedOn)
GO
create index ix_portaluseraudit2 on dbo.PortalUserAudit (ChangedOn,LoginId)
GO

/* PortalUserPropertyAccess */
if exists (select name from dbo.sysobjects where name = 'PortalUserPropertyAccess' and type = 'U')
drop table dbo.PortalUserPropertyAccess
GO

create table dbo.PortalUserPropertyAccess
(
   ID uniqueidentifier not null constraint df_portaluse_id_C46EEA4A default newid(),
   PortalUserID integer not null,
   PortalPropertyID uniqueidentifier not null,
   TapsEnabled nchar(1) not null constraint df_portaluse_tapsenabl_C711A7C2 default 'N',
   OpsEnabled nchar(1) not null constraint df_portaluse_opsenable_D6D18E7E default 'N',
   SurveyEnabled nchar(1) not null constraint df_portaluse_surveyena_596E2A18 default 'N',
   TapsAdmin nchar(1) not null constraint df_portaluse_tapsadmin_F3B74C10 default 'N',
   constraint pk_portaluserpropertyaccess primary key nonclustered (ID),
   constraint ak_portaluserpropertyaccess1 unique (PortalUserID,PortalPropertyID),
   constraint ck_portaluse_tapsenabl_C711A7C2 check(TapsEnabled in ('Y','N')),
   constraint ck_portaluse_opsenable_D6D18E7E check(OpsEnabled in ('Y','N')),
   constraint ck_portaluse_surveyena_596E2A18 check(SurveyEnabled in ('Y','N')),
   constraint ck_portaluse_tapsadmin_F3B74C10 check(TapsAdmin in ('Y','N'))
)
GO

alter table PortalUserPropertyAccess set (lock_escalation = auto)
GO

/* PortalUserRegistration */
if exists (select name from dbo.sysobjects where name = 'PortalUserRegistration' and type = 'U')
drop table dbo.PortalUserRegistration
GO

create table dbo.PortalUserRegistration
(
   ID bigint identity(1,1) not null,
   PortalUserID integer not null,
   ExpiresOn datetime not null,
   RegisteredOn datetime,
   InvitedOn datetime not null,
   InvitedByPortalUserID integer not null,
   NumberVerifyFailures integer not null constraint df_portaluse_numberver_79CF7B9A default 0,
   VerificationCode integer,
   constraint pk_portaluserregistration primary key nonclustered (ID),
   constraint ak_portaluserregistration1 unique (PortalUserID)
)
GO

alter table PortalUserRegistration set (lock_escalation = auto)
GO

/* Product */
if exists (select name from dbo.sysobjects where name = 'Product' and type = 'U')
drop table dbo.Product
GO

create table dbo.Product
(
   ID integer identity(1,1) not null,
   Code nvarchar(25) not null,
   Name nvarchar(200) not null,
   URL nvarchar(255),
   RequiredUserType nvarchar(7),
   RequiredDeviceTypes nvarchar(200),
   constraint pk_product primary key nonclustered (ID),
   constraint ak_product1 unique (Code),
   constraint ak_product2 unique (Name),
   constraint ck_product_requiredu_3E62EB74 check(RequiredUserType in ('LOYALTY','RMS','OTHER'))
)
GO

alter table Product set (lock_escalation = auto)
GO

/* ProtectedApiAccessKey */
if exists (select name from dbo.sysobjects where name = 'ProtectedApiAccessKey' and type = 'U')
drop table dbo.ProtectedApiAccessKey
GO

create table dbo.ProtectedApiAccessKey
(
   AccessKey nvarchar(2000) not null,
   constraint pk_protectedapiaccesskey primary key nonclustered (AccessKey)
)
GO

alter table ProtectedApiAccessKey set (lock_escalation = auto)
GO

/* RemoteServiceProvider */
if exists (select name from dbo.sysobjects where name = 'RemoteServiceProvider' and type = 'U')
drop table dbo.RemoteServiceProvider
GO

create table dbo.RemoteServiceProvider
(
   Name nvarchar(100) not null,
   DatabaseID integer,
   URL nvarchar(255) not null,
   constraint pk_remoteserviceprovider primary key nonclustered (Name,DatabaseID)
)
GO

alter table RemoteServiceProvider set (lock_escalation = auto)
GO

/* SAMLConfig */
if exists (select name from dbo.sysobjects where name = 'SAMLConfig' and type = 'U')
drop table dbo.SAMLConfig
GO

create table dbo.SAMLConfig
(
   ID integer identity(1,1) not null,
   URL nvarchar(200) not null,
   SignatureMethodName nvarchar(50),
   SignatureMethodUri nvarchar(100),
   DigestMethod nvarchar(100),
   ProtocolBinding nvarchar(100),
   SpNameQualifier nvarchar(100),
   AuthnContextClassref nvarchar(100),
   NameidPolicyFormat nvarchar(100),
   KeystoreAlias nvarchar(50),
   UsernameVariable nvarchar(50),
   EmailVariable nvarchar(50),
   PropertyCodeVariable nvarchar(50),
   DisplayNameVariable nvarchar(50),
   EmployeeIdVariable nvarchar(50),
   EnableAutoRegistration nchar(1) not null constraint df_samlconfi_enableaut_F1EA9BBB default 'N',
   SignRequestKeystoreAlias nvarchar(50),
   SignResponseKeystoreAlias nvarchar(50),
   DecryptResponseKeystoreAlias nvarchar(50),
   UserAutoMigrationType nvarchar(max),
   AutoLinkEmployeeForManagersType nvarchar(max),
   UseAltEmpID nchar(1) not null constraint df_samlconfi_usealtemp_87436811 default 'N',
   KeepMeSignedInDeviceTypes nvarchar(max),
   LogOffUrl nvarchar(max),
   ForceAuthn nchar(1) constraint df_samlconfi_forceauth_BBF0840B default 'N',
   constraint pk_samlconfig primary key nonclustered (ID),
   constraint ck_samlconfi_enableaut_F1EA9BBB check(EnableAutoRegistration in ('Y','N')),
   constraint ck_samlconfi_usealtemp_87436811 check(UseAltEmpID in ('Y','N')),
   constraint ck_samlconfi_forceauth_BBF0840B check(ForceAuthn in ('Y','N'))
)
GO

alter table SAMLConfig set (lock_escalation = auto)
GO

/* SAMLIDPConfig */
if exists (select name from dbo.sysobjects where name = 'SAMLIDPConfig' and type = 'U')
drop table dbo.SAMLIDPConfig
GO

create table dbo.SAMLIDPConfig
(
   ID integer identity(1,1) not null,
   PortalClientID integer not null,
   ExternalProductName nvarchar(50),
   ExternalProductCode nvarchar(2),
   SAMLVersion nvarchar(10) not null constraint df_samlidpco_samlversi_7FC06F76 default 2.0,
   PostbackURL nvarchar(max) not null,
   UserNameVariable nvarchar(254) not null constraint df_samlidpco_usernamev_24914087 default 'Username',
   KeystoreAlias nvarchar(25),
   SignRequestKeystoreAlias nvarchar(25),
   SignResponseKeystoreAlias nvarchar(25),
   DecryptResponseKeystoreAlias nvarchar(25),
   constraint pk_samlidpconfig primary key nonclustered (ID),
   constraint ak_samlidpconfig1 unique (PortalClientID,ExternalProductCode)
)
GO

alter table SAMLIDPConfig set (lock_escalation = auto)
GO

/* Session */
if exists (select name from dbo.sysobjects where name = 'Session' and type = 'U')
drop table dbo.Session
GO

create table dbo.Session
(
   ID integer identity(1,1) not null,
   SessionID nvarchar(255) not null,
   Host nvarchar(255) not null,
   SessionTime datetime not null,
   constraint pk_session primary key nonclustered (ID),
   constraint ak_session1 unique (SessionID)
)
GO

alter table Session set (lock_escalation = auto)
GO

create index ix_session1 on dbo.Session (Host)
GO

/* SessionHistory */
if exists (select name from dbo.sysobjects where name = 'SessionHistory' and type = 'U')
drop table dbo.SessionHistory
GO

create table dbo.SessionHistory
(
   ID integer identity(1,1) not null,
   SessionID nvarchar(255) not null,
   Host nvarchar(255) not null,
   LoginID nvarchar(254),
   LoginDT datetime,
   LogoutDT datetime,
   TimedOut nchar(1) constraint df_sessionhi_timedout_9051298 default 'N',
   DatabaseID integer,
   DatabaseName nvarchar(50),
   PropertyID integer,
   PropertyName nvarchar(50),
   constraint pk_sessionhistory primary key nonclustered (ID),
   constraint ak_sessionhistory1 unique (SessionID),
   constraint ck_sessionhi_timedout_9051298 check(TimedOut in ('Y','N'))
)
GO

alter table SessionHistory set (lock_escalation = auto)
GO

create index ix_sessionhistory1 on dbo.SessionHistory (Host)
GO
create index ix_sessionhistory2 on dbo.SessionHistory (LoginID,LoginDT)
GO

/* SystemMessage */
if exists (select name from dbo.sysobjects where name = 'SystemMessage' and type = 'U')
drop table dbo.SystemMessage
GO

create table dbo.SystemMessage
(
   ID integer identity(1,1) not null,
   StartTime datetime not null,
   EndTime datetime not null,
   MsgText nvarchar(max),
   constraint pk_systemmessage primary key nonclustered (ID)
)
GO

alter table SystemMessage set (lock_escalation = auto)
GO

create index ix_systemmessage1 on dbo.SystemMessage (StartTime,EndTime)
GO

/* TermsAndConditionsTextOverride */
if exists (select name from dbo.sysobjects where name = 'TermsAndConditionsTextOverride' and type = 'U')
drop table dbo.TermsAndConditionsTextOverride
GO

create table dbo.TermsAndConditionsTextOverride
(
   ClientID integer not null,
   Locale nvarchar(max) not null,
   DisplayText nvarchar(max) not null,
   constraint pk_termsandconditionstextoverrid primary key nonclustered (ClientID,Locale)
)
GO

alter table TermsAndConditionsTextOverride set (lock_escalation = auto)
GO

/* URLConfig */
if exists (select name from dbo.sysobjects where name = 'URLConfig' and type = 'U')
drop table dbo.URLConfig
GO

create table dbo.URLConfig
(
   Name nvarchar(255) not null,
   URL nvarchar(255) not null,
   constraint pk_urlconfig primary key nonclustered (Name)
)
GO

alter table URLConfig set (lock_escalation = auto)
GO

/* UserData */
if exists (select name from dbo.sysobjects where name = 'UserData' and type = 'U')
drop table dbo.UserData
GO

create table dbo.UserData
(
   ID integer identity(1,1) not null,
   UserID integer not null,
   Tag nvarchar(50) not null,
   TagValue nvarchar(255),
   TagData varbinary(max),
   constraint pk_userdata primary key nonclustered (ID),
   constraint ak_userdata1 unique (UserID,Tag)
)
GO

alter table UserData set (lock_escalation = auto)
GO

/* UserDatabase */
if exists (select name from dbo.sysobjects where name = 'UserDatabase' and type = 'U')
drop table dbo.UserDatabase
GO

create table dbo.UserDatabase
(
   UserID integer not null,
   DatabaseID integer not null,
   constraint pk_userdatabase primary key nonclustered (UserID,DatabaseID)
)
GO

alter table UserDatabase set (lock_escalation = auto)
GO

/* UserFailedPassword */
if exists (select name from dbo.sysobjects where name = 'UserFailedPassword' and type = 'U')
drop table dbo.UserFailedPassword
GO

create table dbo.UserFailedPassword
(
   ID integer identity(1,1) not null,
   UserID integer not null,
   AttemptedOn datetime not null,
   PWord nvarchar(200) not null,
   constraint pk_userfailedpassword primary key nonclustered (ID),
   constraint ak_userfailedpassword1 unique (UserID,AttemptedOn)
)
GO

alter table UserFailedPassword set (lock_escalation = auto)
GO

/* UserPreference */
if exists (select name from dbo.sysobjects where name = 'UserPreference' and type = 'U')
drop table dbo.UserPreference
GO

create table dbo.UserPreference
(
   ID bigint identity(1,1) not null,
   UserID integer not null,
   PrefKey nvarchar(200) not null,
   PrefValue nvarchar(max) not null,
   constraint pk_userpreference primary key nonclustered (ID),
   constraint ak_userpreference1 unique (UserID,PrefKey)
)
GO

alter table UserPreference set (lock_escalation = auto)
GO

/* UserPriorPassword */
if exists (select name from dbo.sysobjects where name = 'UserPriorPassword' and type = 'U')
drop table dbo.UserPriorPassword
GO

create table dbo.UserPriorPassword
(
   ID integer identity(1,1) not null,
   UserID integer not null,
   EffDate datetime not null,
   PWord nvarchar(200) not null,
   constraint pk_userpriorpassword primary key nonclustered (ID),
   constraint ak_userpriorpassword1 unique (UserID,EffDate)
)
GO

alter table UserPriorPassword set (lock_escalation = auto)
GO

/* UserRefreshToken */
if exists (select name from dbo.sysobjects where name = 'UserRefreshToken' and type = 'U')
drop table dbo.UserRefreshToken
GO

create table dbo.UserRefreshToken
(
   UserID integer not null,
   RefreshToken nvarchar(max) not null,
   constraint pk_userrefreshtoken primary key nonclustered (UserID)
)
GO

alter table UserRefreshToken set (lock_escalation = auto)
GO

/* UserRegistration */
if exists (select name from dbo.sysobjects where name = 'UserRegistration' and type = 'U')
drop table dbo.UserRegistration
GO

create table dbo.UserRegistration
(
   ID bigint identity(1,1) not null,
   LoginID nvarchar(254) not null,
   EMail nvarchar(254),
   ClientID integer not null,
   LoginClientID integer,
   DatabaseID integer not null,
   PropertyID integer not null,
   EmployeeID integer not null,
   EmpID nvarchar(25) not null,
   FirstName nvarchar(50) not null,
   MiddleName nvarchar(50) not null,
   LastName nvarchar(50) not null,
   ExpiresOn datetime not null,
   RegisteredOn datetime,
   OptOut nchar(1) not null constraint df_userregis_optout_63E35456 default 'N',
   NumberVerifyFailures integer not null constraint df_userregis_numberver_A730F086 default 0,
   NumberOfAttempts integer not null constraint df_userregis_numberofa_442FD101 default 0,
   Locale nvarchar(10),
   VerificationCode integer,
   CreatedByPortalUserID integer,
   UpdatedByPortalUserID integer,
   constraint pk_userregistration primary key nonclustered (ID),
   constraint ak_userregistration1 unique (LoginID,ClientID,DatabaseID,PropertyID,EmployeeID),
   constraint ck_userregis_optout_63E35456 check(OptOut in ('Y','N'))
)
GO

alter table UserRegistration set (lock_escalation = auto)
GO

/* WatsonDatabase */
if exists (select name from dbo.sysobjects where name = 'WatsonDatabase' and type = 'U')
drop table dbo.WatsonDatabase
GO

create table dbo.WatsonDatabase
(
   ID integer identity(1,1) not null,
   Name nvarchar(50) not null,
   DatabaseURL nvarchar(255) not null,
   DatabaseUserName nvarchar(200) not null,
   DatabasePassword nvarchar(200) not null,
   DatabaseDriver nvarchar(255) not null,
   RedirectURL nvarchar(255),
   MobileRedirectURL nvarchar(255),
   ApiURL nvarchar(255),
   LoginURL nvarchar(255),
   WatsonVersion nvarchar(10),
   OlapDatabaseURL nvarchar(255),
   OlapDatabaseUserName nvarchar(200),
   OlapDatabasePassword nvarchar(200),
   constraint pk_watsondatabase primary key nonclustered (ID),
   constraint ak_watsondatabase1 unique (Name)
)
GO

alter table WatsonDatabase set (lock_escalation = auto)
GO

/* relations */
alter table dbo.AccessURL add constraint fk_accessurl1 foreign key (PortalClientID) references dbo.PortalClient(ID) on delete no action
GO
alter table dbo.ApiAuthorization add constraint fk_apiauthorization1 foreign key (PortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.AutomationAuthorization add constraint fk_automationauthorization1 foreign key (PortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.ClientPreference add constraint fk_clientpreference1 foreign key (PortalClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.ClientProduct add constraint fk_clientproduct1 foreign key (ClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.ClientProduct add constraint fk_clientproduct2 foreign key (ProductID) references dbo.Product(ID) on delete cascade
GO
alter table dbo.ClientRegion add constraint fk_clientregion1 foreign key (PortalClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.ImpersonateHistory add constraint fk_impersonatehistory1 foreign key (PortalUserIDToImpersonate) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.ImpersonateHistory add constraint fk_impersonatehistory2 foreign key (PortalUserIDOfImpersonator) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.LoginHistory add constraint fk_loginhistory1 foreign key (PortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.LoginHistory add constraint fk_loginhistory2 foreign key (DatabaseID) references dbo.WatsonDatabase(ID) on delete cascade
GO
alter table dbo.NewUserRegistration add constraint fk_newuserregistration1 foreign key (ClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.NewUserRegistration add constraint fk_newuserregistration2 foreign key (LoginClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.NewUserRegistration add constraint fk_newuserregistration3 foreign key (CreatedByPortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.NewUserRegistration add constraint fk_newuserregistration4 foreign key (UpdatedByPortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.NewUserRegistration add constraint fk_newuserregistration5 foreign key (DatabaseID) references dbo.WatsonDatabase(ID) on delete cascade
GO
alter table dbo.NewUserRegistration add constraint fk_newuserregistration6 foreign key (PortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.PortalAdminUserAdditionalClients add constraint fk_portaladminuseradditionalcli1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.PortalAdminUserAdditionalClients add constraint fk_portaladminuseradditionalcli2 foreign key (ClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.PortalClient add constraint fk_portalclient1 foreign key (ParentClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.PortalClient add constraint fk_portalclient2 foreign key (DatabaseID) references dbo.WatsonDatabase(ID) on delete set null
GO
alter table dbo.PortalClient add constraint fk_portalclient3 foreign key (PasswordRulesID) references dbo.PasswordRules(ID) on delete set null
GO
alter table dbo.PortalClient add constraint fk_portalclient4 foreign key (LDAPID) references dbo.LDAPConfig(ID) on delete set null
GO
alter table dbo.PortalClient add constraint fk_portalclient5 foreign key (SAMLID) references dbo.SAMLConfig(ID) on delete set null
GO
alter table dbo.PortalClient add constraint fk_portalclient6 foreign key (InviteLoginClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.PortalClient add constraint fk_portalclient7 foreign key (ManagerInviteLoginClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.PortalProperty add constraint fk_portalproperty1 foreign key (PortalClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.PortalProperty add constraint fk_portalproperty2 foreign key (TapsDatabaseID) references dbo.WatsonDatabase(ID) on delete set null
GO
alter table dbo.PortalProperty add constraint fk_portalproperty3 foreign key (BrandID) references dbo.Brand(ID) on delete set null
GO
alter table dbo.PortalProperty add constraint fk_portalproperty4 foreign key (ClientRegionID) references dbo.ClientRegion(ID) on delete set null
GO
alter table dbo.PortalPropertyAdditionalLocale add constraint fk_portalpropertyadditionalloca1 foreign key (PortalPropertyID) references dbo.PortalProperty(ID) on delete cascade
GO
alter table dbo.PortalUser add constraint fk_portaluser1 foreign key (QuestionID) references dbo.PasswordQuestion(ID) on delete no action
GO
alter table dbo.PortalUser add constraint fk_portaluser2 foreign key (LoginClientID) references dbo.PortalClient(ID) on delete no action
GO
alter table dbo.PortalUser add constraint fk_portaluser3 foreign key (ClientID) references dbo.PortalClient(ID) on delete no action
GO
alter table dbo.PortalUser add constraint fk_portaluser4 foreign key (DefaultDatabaseID) references dbo.WatsonDatabase(ID) on delete set null
GO
alter table dbo.PortalUser add constraint fk_portaluser5 foreign key (CreatedByPortalUserID) references dbo.PortalUser(ID) on delete set null
GO
alter table dbo.PortalUserPropertyAccess add constraint fk_portaluserpropertyaccess1 foreign key (PortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.PortalUserPropertyAccess add constraint fk_portaluserpropertyaccess2 foreign key (PortalPropertyID) references dbo.PortalProperty(ID) on delete cascade
GO
alter table dbo.PortalUserRegistration add constraint fk_portaluserregistration1 foreign key (PortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.PortalUserRegistration add constraint fk_portaluserregistration2 foreign key (InvitedByPortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.SAMLIDPConfig add constraint fk_samlidpconfig1 foreign key (PortalClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.TermsAndConditionsTextOverride add constraint fk_termsandconditionstextoverri1 foreign key (ClientID) references dbo.PortalClient(ID) on delete cascade
GO
alter table dbo.UserData add constraint fk_userdata1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserDatabase add constraint fk_userdatabase1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserDatabase add constraint fk_userdatabase2 foreign key (DatabaseID) references dbo.WatsonDatabase(ID) on delete cascade
GO
alter table dbo.UserFailedPassword add constraint fk_userfailedpassword1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserPreference add constraint fk_userpreference1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserPriorPassword add constraint fk_userpriorpassword1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserRefreshToken add constraint fk_userrefreshtoken1 foreign key (UserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserRegistration add constraint fk_userregistration1 foreign key (CreatedByPortalUserID) references dbo.PortalUser(ID) on delete cascade
GO
alter table dbo.UserRegistration add constraint fk_userregistration2 foreign key (UpdatedByPortalUserID) references dbo.PortalUser(ID) on delete cascade
GO

/* portaluser_delete */
if exists (select name from dbo.sysobjects where name = 'portaluser_delete' and type = 'TR')
   drop trigger portaluser_delete
GO

create trigger portaluser_delete on dbo.PortalUser for delete as
if (select count(*) from deleted) > 0
BEGIN
   update dbo.PortalUser set CreatedByPortalUserID = null where CreatedByPortalUserID in (select ID from deleted);
   delete from dbo.ApiAuthorization where PortalUserID in (select ID from deleted);
   delete from dbo.AutomationAuthorization where PortalUserID in (select ID from deleted);
   delete from dbo.ImpersonateHistory where PortalUserIDToImpersonate in (select ID from deleted);
   delete from dbo.ImpersonateHistory where PortalUserIDOfImpersonator in (select ID from deleted);
   delete from dbo.LoginHistory where PortalUserID in (select ID from deleted);
   delete from dbo.NewUserRegistration where CreatedByPortalUserID in (select ID from deleted);
   delete from dbo.NewUserRegistration where UpdatedByPortalUserID in (select ID from deleted);
   delete from dbo.NewUserRegistration where PortalUserID in (select ID from deleted);
   delete from dbo.PortalAdminUserAdditionalClients where UserID in (select ID from deleted);
   delete from dbo.PortalUserPropertyAccess where PortalUserID in (select ID from deleted);
   delete from dbo.PortalUserRegistration where PortalUserID in (select ID from deleted);
   delete from dbo.PortalUserRegistration where InvitedByPortalUserID in (select ID from deleted);
   delete from dbo.UserData where UserID in (select ID from deleted);
   delete from dbo.UserDatabase where UserID in (select ID from deleted);
   delete from dbo.UserFailedPassword where UserID in (select ID from deleted);
   delete from dbo.UserPreference where UserID in (select ID from deleted);
   delete from dbo.UserPriorPassword where UserID in (select ID from deleted);
   delete from dbo.UserRefreshToken where UserID in (select ID from deleted);
   delete from dbo.UserRegistration where CreatedByPortalUserID in (select ID from deleted);
   delete from dbo.UserRegistration where UpdatedByPortalUserID in (select ID from deleted);
   declare @current_statement varchar(MAX);

               declare @TEMP table (EventType nvarchar(30), Parameters int, EventInfo nvarchar(4000));
               insert into @TEMP EXEC('DBCC INPUTBUFFER(@@SPID) with NO_INFOMSGS');
               select @current_statement = EventInfo from @TEMP;

               declare @portal_user_id int;  -- TODO: Add support

               insert into PortalUserAudit (PortalUserID,
                                            LoginClientID,
                                            LoginId,
                                            PWord,
                                            Active,
                                            FirstName,
                                            MiddleName,
                                            LastName,
                                            EMail,
                                            MobilePhone,
                                            QuestionID,
                                            Answer,
                                            LastLogin,
                                            PwordChanged,
                                            Locale,
                                            ClientID,
                                            DefaultDatabaseID,
                                            BypassLDAP,
                                            LdapUserName,
                                            Flags,
                                            ChangedOn,
                                            Operation,
                                            Command,
                                            HostName,
                                            ChangedBySqlUser,
                                            ChangedByPortalUserID,
                                            CanAdministerUsers)
                      select
                         d.ID,
                         d.LoginClientID,
                         d.LoginId,
                         d.PWord,
                         d.Active,
                         d.FirstName,
                         d.MiddleName,
                         d.LastName,
                         d.EMail,
                         d.MobilePhone,
                         d.QuestionID,
                         d.Answer,
                         d.LastLogin,
                         d.PwordChanged,
                         d.Locale,
                         d.ClientID,
                         d.DefaultDatabaseID,
                         d.BypassLDAP,
                         d.LdapUserName,
                         d.Flags,
                         GETUTCDATE(),
                         'D',
                         @current_statement,
                         HOST_NAME(),
                         SYSTEM_USER,
                         @portal_user_id,
                         d.CanAdministerUsers
                      from deleted d;
END
GO

/* portaluser_update */
if exists (select name from dbo.sysobjects where name = 'portaluser_update' and type = 'TR')
   drop trigger portaluser_update
GO

create trigger portaluser_update on dbo.PortalUser for insert,update as
BEGIN
   if update(QuestionID)
   begin
      if (select count(*) from Inserted where QuestionID is not null) > 0
      begin
         if (select count(*) from dbo.PasswordQuestion p,Inserted i where p.ID = i.QuestionID) = 0
         begin
            rollback transaction
            raiserror ('The QuestionID''s value doesn''t exist in the PasswordQuestion table.', 16, 1)
            return
         end
      end
   end;

   if update(LoginClientID)
   begin
      if (select count(*) from Inserted where LoginClientID is not null) > 0
      begin
         if (select count(*) from dbo.PortalClient p,Inserted i where p.ID = i.LoginClientID) = 0
         begin
            rollback transaction
            raiserror ('The LoginClientID''s value doesn''t exist in the PortalClient table.', 16, 1)
            return
         end
      end
   end;

   if update(ClientID)
   begin
      if (select count(*) from Inserted where ClientID is not null) > 0
      begin
         if (select count(*) from dbo.PortalClient p,Inserted i where p.ID = i.ClientID) = 0
         begin
            rollback transaction
            raiserror ('The ClientID''s value doesn''t exist in the PortalClient table.', 16, 1)
            return
         end
      end
   end;

   if update(DefaultDatabaseID)
   begin
      if (select count(*) from Inserted where DefaultDatabaseID is not null) > 0
      begin
         if (select count(*) from dbo.WatsonDatabase p,Inserted i where p.ID = i.DefaultDatabaseID) = 0
         begin
            rollback transaction
            raiserror ('The DefaultDatabaseID''s value doesn''t exist in the WatsonDatabase table.', 16, 1)
            return
         end
      end
   end;

   if update(CreatedByPortalUserID)
   begin
      if (select count(*) from Inserted where CreatedByPortalUserID is not null) > 0
      begin
         if (select count(*) from dbo.PortalUser p,Inserted i where p.ID = i.CreatedByPortalUserID) = 0
         begin
            rollback transaction
            raiserror ('The CreatedByPortalUserID''s value doesn''t exist in the PortalUser table.', 16, 1)
            return
         end
      end
   end;

   declare @current_statement varchar(MAX);

               declare @TEMP table (EventType nvarchar(30), Parameters int, EventInfo nvarchar(4000));
               insert into @TEMP EXEC('DBCC INPUTBUFFER(@@SPID) with NO_INFOMSGS');
               select @current_statement = EventInfo from @TEMP;

               declare @operation char(1) = (case when (select COUNT(*) from deleted) = 0 then 'I' else 'U' end);

               declare @portal_user_id int;  -- TODO: Add support

               if @Operation = 'I'
                  insert into PortalUserAudit (PortalUserID,
                                               LoginClientID,
                                               LoginId,
                                               PWord,
                                               Active,
                                               FirstName,
                                               MiddleName,
                                               LastName,
                                               EMail,
                                               MobilePhone,
                                               QuestionID,
                                               Answer,
                                               LastLogin,
                                               PwordChanged,
                                               Locale,
                                               ClientID,
                                               DefaultDatabaseID,
                                               BypassLDAP,
                                               LdapUserName,
                                               Flags,
                                               ChangedOn,
                                               Operation,
                                               Command,
                                               HostName,
                                               ChangedBySqlUser,
                                               ChangedByPortalUserID,
                                               CanAdministerUsers)
                      select
                         i.ID,
                         i.LoginClientID,
                         i.LoginId,
                         i.PWord,
                         i.Active,
                         i.FirstName,
                         i.MiddleName,
                         i.LastName,
                         i.EMail,
                         i.MobilePhone,
                         i.QuestionID,
                         i.Answer,
                         i.LastLogin,
                         i.PwordChanged,
                         i.Locale,
                         i.ClientID,
                         i.DefaultDatabaseID,
                         i.BypassLDAP,
                         i.LdapUserName,
                         i.Flags,
                         GETUTCDATE(),
                         @operation,
                         @current_statement,
                         HOST_NAME(),
                         SYSTEM_USER,
                         @portal_user_id,
                         i.CanAdministerUsers
                      from inserted i
               else
                  insert into PortalUserAudit (PortalUserID,
                                               LoginClientID,
                                               LoginId,
                                               PWord,
                                               Active,
                                               FirstName,
                                               MiddleName,
                                               LastName,
                                               EMail,
                                               MobilePhone,
                                               QuestionID,
                                               Answer,
                                               LastLogin,
                                               PwordChanged,
                                               Locale,
                                               ClientID,
                                               DefaultDatabaseID,
                                               BypassLDAP,
                                               LdapUserName,
                                               Flags,
                                               ChangedOn,
                                               Operation,
                                               Command,
                                               HostName,
                                               ChangedBySqlUser,
                                               ChangedByPortalUserID,
                                               CanAdministerUsers)
                      select
                         i.ID,
                         i.LoginClientID,
                         i.LoginId,
                         i.PWord,
                         i.Active,
                         i.FirstName,
                         i.MiddleName,
                         i.LastName,
                         i.EMail,
                         i.MobilePhone,
                         i.QuestionID,
                         i.Answer,
                         i.LastLogin,
                         i.PwordChanged,
                         i.Locale,
                         i.ClientID,
                         i.DefaultDatabaseID,
                         i.BypassLDAP,
                         i.LdapUserName,
                         i.Flags,
                         GETUTCDATE(),
                         @operation,
                         @current_statement,
                         HOST_NAME(),
                         SYSTEM_USER,
                         @portal_user_id,
                         i.CanAdministerUsers
                      from inserted i
                           join deleted d on
                         i.ID = d.ID and
                         COALESCE(i.LastLogin, '') = COALESCE(d.LastLogin, '') and (
                           COALESCE(i.PWord, '') <> COALESCE(d.PWord, '') or
                           COALESCE(i.EMail, '') <> COALESCE(d.EMail, '') or
                           COALESCE(i.Flags, 0) <> COALESCE(d.Flags, 0) or
                           COALESCE(i.Active, '') <> COALESCE(d.Active, '') or
                           COALESCE(i.QuestionID, 0) <> COALESCE(d.QuestionID, 0) or
                           COALESCE(i.Locale, '') <> COALESCE(d.Locale, '') or
                           COALESCE(i.ClientID, 0) <> COALESCE(d.ClientID, 0) or
                           COALESCE(i.DefaultDatabaseID, 0) <> COALESCE(d.DefaultDatabaseID, 0) or
                           COALESCE(i.BypassLdap, '') <> COALESCE(d.BypassLdap, '') or
                           COALESCE(i.LoginClientID, 0) <> COALESCE(d.LoginClientID, 0) or
                           COALESCE(i.MobilePhone, '') <> COALESCE(d.MobilePhone, '') or
                           COALESCE(i.FirstName, '') <> COALESCE(d.FirstName, '') or
                           COALESCE(i.MiddleName, '') <> COALESCE(d.MiddleName, '') or
                           COALESCE(i.LastName, '') <> COALESCE(d.LastName, '') or
                           COALESCE(i.LdapUserName, '') <> COALESCE(d.LdapUserName, '') or
                           COALESCE(i.PwordChangedSameDayCount, 0) <> COALESCE(d.PwordChangedSameDayCount, 0) );
END
GO

/* dbo.UserDatabaseQuery */
if exists (select name from dbo.sysobjects where name = 'UserDatabaseQuery' and type = 'V')
   drop view dbo.UserDatabaseQuery
GO
create view dbo.UserDatabaseQuery as
   select u.ID as UserID,u.LoginId as LoginId,u.EMail,u.Flags,d.ID as DatabaseID,d.Name as DatabaseName,d.DatabaseURL,d.DatabaseUserName,d.DatabasePassword,d.DatabaseDriver,d.RedirectURL from PortalUser u,WatsonDatabase d,UserDatabase ud where u.ID = ud.UserID and d.ID = ud.DatabaseID
GO

