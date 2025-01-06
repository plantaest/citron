export const sortObjectByKey = (obj: object) =>
  Object.fromEntries(Object.entries(obj).sort(([keyA], [keyB]) => keyA.localeCompare(keyB)));
