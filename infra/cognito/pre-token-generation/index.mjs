const CLAIM_MAPPINGS = [
  ["custom:tenant_id", "tenant_id"],
  ["custom:tenant_type", "tenant_type"],
  ["custom:user_status", "user_status"],
];

export const handler = async (event) => {
  const userAttributes = event.request?.userAttributes ?? {};
  const claimsToAddOrOverride = buildClaims(event, userAttributes);

  if (Object.keys(claimsToAddOrOverride).length === 0) {
    console.log("No custom tenant claims found for user.", {
      userName: event.userName,
      version: event.version,
    });
    return event;
  }

  if (supportsAccessTokenCustomization(event.version)) {
    event.response = event.response ?? {};
    event.response.claimsAndScopeOverrideDetails = {
      idTokenGeneration: {
        claimsToAddOrOverride,
      },
      accessTokenGeneration: {
        claimsToAddOrOverride,
      },
      groupOverrideDetails: event.request?.groupConfiguration,
    };
    return event;
  }

  event.response = event.response ?? {};
  event.response.claimsOverrideDetails = {
    claimsToAddOrOverride,
    groupOverrideDetails: event.request?.groupConfiguration,
  };
  return event;
};

function buildClaims(event, userAttributes) {
  const claims = {};

  for (const [attributeName, apiClaimName] of CLAIM_MAPPINGS) {
    const value = normalize(userAttributes[attributeName]);
    if (!value) {
      continue;
    }

    claims[attributeName] = value;
    claims[apiClaimName] = value;
  }

  const username = normalize(event.userName);
  if (username) {
    claims.username = username;
  }

  const email = normalize(userAttributes.email);
  if (email) {
    claims.email = email;
  }

  return claims;
}

function supportsAccessTokenCustomization(version) {
  return version === "2" || version === "2.0" || version === "V2_0" || version === "3" || version === "3.0" || version === "V3_0";
}

function normalize(value) {
  if (typeof value !== "string") {
    return null;
  }

  const trimmedValue = value.trim();
  return trimmedValue.length > 0 ? trimmedValue : null;
}
