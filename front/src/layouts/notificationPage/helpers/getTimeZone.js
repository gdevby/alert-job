const getSign = (timezoneOffset) => {
  return Math.sign(timezoneOffset) == 1 ? '-' : '+'
}

export const getTimeZone = () => {
  const now = new Date();
  const timezoneOffset = now.getTimezoneOffset();

  const offsetHours = String(Math.floor(Math.abs(timezoneOffset / 60))).padStart(2, '0');
  const offsetMinutes = String(Math.abs(timezoneOffset % 60)).padStart(2, '0');

  const timeZone = `${getSign(timezoneOffset)}${offsetHours}:${offsetMinutes}`;

  return timeZone;
}
