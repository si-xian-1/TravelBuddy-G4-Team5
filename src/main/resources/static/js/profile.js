'use strict';
const byId = id => document.getElementById(id);
const form = byId('profile-form');
const fields = byId('profile-fields');
const statusBox = byId('status');
const apiBase = document.body.dataset.apiBase || '/api/travelers/users';
let profile = null;
let activeId = null;
let busy = false;
let dirty = false;

function status(message, error = false) {
    statusBox.textContent = message;
    statusBox.classList.toggle('error', error);
}
function setBusy(value) {
    busy = value;
    fields.disabled = value || !profile;
    byId('lookup').querySelector('button').disabled = value;
    byId('traveler-id').disabled = value;
    form.setAttribute('aria-busy', String(value));
}
function populate() {
    const contact = profile.emergencyContact || {};
    byId('username').value = profile.username || '';
    byId('mobile').value = profile.mobileNumber || '';
    byId('contact-name').value = contact.name || '';
    byId('contact-mobile').value = contact.mobileNumber || '';
    byId('consent').checked = profile.hasConsentLocation === true;
    byId('password').value = '';
    byId('confirm-password').value = '';
    byId('profile-name').textContent = profile.username || 'Traveler';
    byId('profile-mobile').textContent = profile.mobileNumber || 'No mobile number';
    byId('profile-id').textContent = '#' + activeId;
    byId('profile-consent').textContent = profile.hasConsentLocation ? 'Enabled' : 'Disabled';
    byId('avatar').textContent = (profile.username || 'TB').slice(0, 2).toUpperCase();
    byId('save-note').textContent = 'Your details are up to date.';
    dirty = false;
}
async function request(url, options) {
    const response = await fetch(url, options);
    if (!response.ok) {
        const messages = {400: 'Some details are invalid. Please check your entries.', 401: 'Please sign in to continue.', 403: 'You don’t have permission to access this profile.', 404: 'Traveler not found. Check the traveler ID.', 409: 'That username or mobile number is already in use.'};
        let detail;
        try { const body = await response.json(); detail = body.detail || body.message; } catch (_) { /* Use a readable fallback. */ }
        throw new Error(response.status < 500 && detail ? detail : messages[response.status] || 'Unable to access your profile. Please try again later.');
    }
    return response;
}
async function load(id) {
    if (busy) return;
    if (!/^\d+$/.test(String(id)) || !Number.isSafeInteger(Number(id)) || Number(id) < 1 || Number(id) > 2147483647) {
        status('Enter a valid positive traveler ID.', true); return;
    }
    if (dirty && !confirm('Discard your unsaved changes and load another profile?')) return;
    setBusy(true);
    status('Loading your profile…');
    try {
        const response = await request(apiBase + '/' + Number(id));
        profile = await response.json();
        activeId = Number(id);
        byId('traveler-id').value = activeId;
        populate();
        const url = new URL(location.href);
        url.searchParams.set('userId', activeId);
        history.replaceState(null, '', url);
        status('Profile loaded. You can update your details below.');
    } catch (error) {
        status(error instanceof TypeError ? 'Could not connect. Check your connection and try again.' : error.message, true);
        if (activeId !== null) byId('traveler-id').value = activeId;
    } finally { setBusy(false); }
}
byId('lookup').addEventListener('submit', event => { event.preventDefault(); load(byId('traveler-id').value); });
form.addEventListener('input', () => { dirty = true; byId('save-note').textContent = 'You have unsaved changes.'; });
byId('cancel').addEventListener('click', () => { populate(); status('Changes discarded.'); });
window.addEventListener('beforeunload', event => { if (dirty) { event.preventDefault(); event.returnValue = ''; } });
form.addEventListener('submit', async event => {
    event.preventDefault();
    if (busy || !profile) return;
    const payload = {};
    const contact = {};
    try {
        for (const [id, key, original, target] of [
            ['username', 'username', profile.username, payload],
            ['mobile', 'mobileNumber', profile.mobileNumber, payload],
            ['contact-name', 'name', profile.emergencyContact?.name, contact],
            ['contact-mobile', 'mobileNumber', profile.emergencyContact?.mobileNumber, contact]
        ]) {
            const value = byId(id).value.trim();
            if (value !== (original || '')) {
                if (!value) { byId(id).focus(); throw new Error('Existing details cannot be cleared. Enter a replacement value or discard changes.'); }
                target[key] = value;
            }
        }
        if (Object.keys(contact).length) payload.emergencyContact = contact;
        if (byId('consent').checked !== (profile.hasConsentLocation === true)) payload.hasConsentLocation = byId('consent').checked;
        const password = byId('password').value;
        if (password !== byId('confirm-password').value) throw new Error('Your new passwords don’t match.');
        if (password) {
            if (!password.trim() || password === '........') throw new Error('Choose a password other than blank spaces or the displayed dots.');
            payload.password = password;
        }
        if (!Object.keys(payload).length) { dirty = false; byId('save-note').textContent = 'Your details are up to date.'; status('No changes to save.'); return; }
        setBusy(true);
        status('Saving your changes…');
        await request(apiBase + '/' + activeId, {method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)});
        // Update the visible snapshot only after the server confirms success.
        const {password: ignoredPassword, emergencyContact, ...details} = payload;
        profile = {...profile, ...details, emergencyContact: {...(profile.emergencyContact || {}), ...(emergencyContact || {})}};
        populate();
        status('Your profile has been updated.');
    } catch (error) {
        status(error instanceof TypeError ? 'Could not connect. Your changes are still here; please try again.' : error.message, true);
    } finally { setBusy(false); }
});
const initialId = new URLSearchParams(location.search).get('userId');
if (initialId) { byId('traveler-id').value = initialId; load(initialId); }
else status('Enter your traveler ID to view and edit your profile.');
